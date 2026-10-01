package bf.formation.assoue.signalement.service;

import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.service.EntrepriseService;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import bf.formation.assoue.signalement.dto.SignalementCreateDTO;
import bf.formation.assoue.signalement.dto.SignalementMarketplaceDTO;
import bf.formation.assoue.signalement.dto.SignalementResponseDTO;
import bf.formation.assoue.signalement.exception.AccesRefuseException;
import bf.formation.assoue.signalement.exception.SignalementDejaPrisException;
import bf.formation.assoue.signalement.exception.SignalementNotFoundException;
import bf.formation.assoue.signalement.mapper.SignalementMapper;
import bf.formation.assoue.signalement.model.Signalement;
import bf.formation.assoue.signalement.model.StatutSignalement;
import bf.formation.assoue.signalement.repository.SignalementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalementService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SignalementRepository signalementRepository;
    private final SignalementMapper signalementMapper;
    private final SmsGatewayService smsGatewayService;
    private final EntrepriseService entrepriseService;
    private final GeocodingService geocodingService;

    /**
     * BF-SIG-01, 02, 03, 04 : creation puis matching.
     * PNEU est un cas particulier : ce type est rattache automatiquement au
     * compte interne d'As'Soue (pas de mise en concurrence marketplace, cf.
     * ProfilEntreprise.estAssoue). Les autres types passent par le matching
     * marketplace classique : toutes les entreprises du bon type sont
     * informees par SMS, les plus proches en priorite (envoyees en premier).
     */
    public SignalementResponseDTO creer(Long utilisateurId, SignalementCreateDTO requete) {
        String numeroSuivi = "SIG-" + System.currentTimeMillis() % 1_000_000 + "-" + (100 + RANDOM.nextInt(900));

        Signalement signalement = Signalement.builder()
                .utilisateurId(utilisateurId)
                .typeDechet(requete.getTypeDechet())
                .photoUrl(requete.getPhotoUrl())
                .latitude(requete.getLatitude())
                .longitude(requete.getLongitude())
                .adresse(geocodingService.reverse(requete.getLatitude(), requete.getLongitude()).orElse(null))
                .numeroSuivi(numeroSuivi)
                .statut(StatutSignalement.EN_ATTENTE)
                .build();
        signalement = signalementRepository.save(signalement);

        if (signalement.getTypeDechet() == TypeDechet.PNEU) {
            rattacherAssoue(signalement);
        } else {
            notifierEntreprisesProches(signalement);
        }

        return signalementMapper.toDto(signalementRepository.save(signalement));
    }

    /**
     * PNEU : rattachement direct et automatique au compte As'Soue, sans passer
     * par le marketplace concurrentiel des autres types de dechets.
     */
    private void rattacherAssoue(Signalement signalement) {
        Optional<EntrepriseResponseDTO> assoue = entrepriseService.obtenirCompteAssoue();
        if (assoue.isEmpty()) {
            // Aucun compte As'Soue enregistre pour l'instant (cf. EntrepriseCreateDTO.estAssoue) :
            // le signalement reste EN_ATTENTE plutot que de bloquer sa creation.
            log.warn("Signalement PNEU {} cree mais aucun compte As'Soue (estAssoue=true) n'existe encore",
                    signalement.getNumeroSuivi());
            return;
        }

        EntrepriseResponseDTO entreprise = assoue.get();
        signalement.setEntrepriseId(entreprise.getId());
        signalement.setStatut(StatutSignalement.PRIS_EN_CHARGE);

        try {
            SmsRequestDTO sms = new SmsRequestDTO();
            sms.setTelephone(entreprise.getTelephone());
            sms.setType("ALERTE_INTERNE");
            sms.setContenu("Nouveau signalement pneu " + signalement.getNumeroSuivi()
                    + " vous a ete automatiquement attribue");
            smsGatewayService.envoyer(sms);
        } catch (Exception e) {
            log.warn("Notification SMS echouee pour le compte As'Soue (signalement {})", signalement.getNumeroSuivi(), e);
        }
    }

    /**
     * Types autres que PNEU : toutes les entreprises du bon type sont informees,
     * mais celles les plus proches de la zone du signalement sont contactees en
     * priorite (envoyees en premier dans la boucle, distance rappelee dans le SMS).
     */
    private void notifierEntreprisesProches(Signalement signalement) {
        List<EntrepriseResponseDTO> entreprises;
        try {
            entreprises = entrepriseService.listerParType(signalement.getTypeDechet());
        } catch (Exception e) {
            log.warn("Impossible de recuperer les entreprises pour le type {}", signalement.getTypeDechet(), e);
            return;
        }

        entreprises.stream()
                .sorted(Comparator.comparingDouble(entreprise -> DistanceUtil.distanceKm(
                        signalement.getLatitude(), signalement.getLongitude(),
                        entreprise.getLatitude(), entreprise.getLongitude())))
                .forEach(entreprise -> {
                    double distance = DistanceUtil.distanceKm(
                            signalement.getLatitude(), signalement.getLongitude(),
                            entreprise.getLatitude(), entreprise.getLongitude());
                    try {
                        SmsRequestDTO sms = new SmsRequestDTO();
                        sms.setTelephone(entreprise.getTelephone());
                        sms.setType("ALERTE_INTERNE");
                        sms.setContenu("Nouveau signalement " + signalement.getNumeroSuivi() + " ("
                                + signalement.getTypeDechet() + ") a "
                                + Math.round(distance * 10) / 10.0 + " km de vous");
                        smsGatewayService.envoyer(sms);
                    } catch (Exception e) {
                        log.warn("Notification SMS echouee pour l'entreprise {}", entreprise.getId(), e);
                    }
                });
    }

    /** BF-SIG-05 : historique et statut des signalements de l'utilisateur connecte. */
    public List<SignalementResponseDTO> mesSignalements(Long utilisateurId) {
        return signalementRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId).stream()
                .map(signalementMapper::toDto)
                .toList();
    }

        /**
         * Fil "marketplace" d'une entreprise : signalements EN_ATTENTE de son type,
         * les plus proches en premier. Le compte interne As'Soue voit aussi ses
         * signalements automatiquement attribues, notamment les PNEU.
         */
    public List<SignalementMarketplaceDTO> fluxEntreprise(Long entrepriseId) {
        EntrepriseResponseDTO entreprise = entrepriseService.obtenir(entrepriseId);

        List<Signalement> disponibles = entreprise.getTypesDechetGeres().stream()
                .flatMap(type -> signalementRepository
                        .findByTypeDechetAndStatut(type, StatutSignalement.EN_ATTENTE).stream())
            .collect(java.util.stream.Collectors.toMap(Signalement::getId, s -> s, (premier, ignore) -> premier))
            .values().stream()
            .collect(java.util.stream.Collectors.toList());

        if (entreprise.isEstAssoue()) {
            disponibles.addAll(signalementRepository
                .findByEntrepriseIdAndStatut(entrepriseId, StatutSignalement.PRIS_EN_CHARGE));
        }

        return disponibles.stream().distinct()
            .map(s -> SignalementMarketplaceDTO.builder()
                        .id(s.getId())
                        .typeDechet(s.getTypeDechet())
                        .photoUrl(s.getPhotoUrl())
                        .latitude(s.getLatitude())
                        .longitude(s.getLongitude())
                        .adresse(s.getAdresse())
                        .numeroSuivi(s.getNumeroSuivi())
                        .statut(s.getStatut())
                        .distanceKm(DistanceUtil.distanceKm(
                                s.getLatitude(), s.getLongitude(),
                                entreprise.getLatitude(), entreprise.getLongitude()))
                        .dateCreation(s.getDateCreation())
                        .build())
                .sorted(Comparator.comparingDouble(SignalementMarketplaceDTO::getDistanceKm))
                .toList();
    }

    /** Marketplace : premiere entreprise qui clique le recupere (BF-SIG-04 evolution). */
    public SignalementResponseDTO prendreEnCharge(Long id, Long entrepriseId) {
        Signalement signalement = trouver(id);
        if (signalement.getStatut() != StatutSignalement.EN_ATTENTE) {
            throw new SignalementDejaPrisException(id);
        }
        signalement.setEntrepriseId(entrepriseId);
        signalement.setStatut(StatutSignalement.PRIS_EN_CHARGE);
        return signalementMapper.toDto(signalementRepository.save(signalement));
    }

    /** Marque le signalement comme physiquement collecte (uniquement par l'entreprise qui l'a pris). */
    public SignalementResponseDTO marquerCollecte(Long id, Long entrepriseId) {
        Signalement signalement = trouver(id);
        if (!entrepriseId.equals(signalement.getEntrepriseId())) {
            throw new AccesRefuseException("Ce signalement n'a pas ete pris en charge par votre entreprise");
        }
        signalement.setStatut(StatutSignalement.COLLECTE);
        return signalementMapper.toDto(signalementRepository.save(signalement));
    }

    /** BF-BO-02 : liste complete pour la carte interactive du back-office (role ADMIN). */
    public List<SignalementResponseDTO> tousLesSignalements() {
        return signalementRepository.findAll().stream().map(signalementMapper::toDto).toList();
    }

    /** BF-BO-02 : mise a jour manuelle du statut par l'administrateur (cas exceptionnels). */
    public SignalementResponseDTO changerStatut(Long id, StatutSignalement statut) {
        Signalement signalement = trouver(id);
        signalement.setStatut(statut);
        return signalementMapper.toDto(signalementRepository.save(signalement));
    }

    private Signalement trouver(Long id) {
        return signalementRepository.findById(id).orElseThrow(() -> new SignalementNotFoundException(id));
    }
}
