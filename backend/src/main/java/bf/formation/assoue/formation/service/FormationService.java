package bf.formation.assoue.formation.service;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.auth.service.AuthService;
import bf.formation.assoue.formation.dto.FormationCreateDTO;
import bf.formation.assoue.formation.dto.FormationResponseDTO;
import bf.formation.assoue.formation.dto.InscriptionFormationResponseDTO;
import bf.formation.assoue.formation.exception.DejaInscritFormationException;
import bf.formation.assoue.formation.exception.FormationCompleteException;
import bf.formation.assoue.formation.exception.FormationNotFoundException;
import bf.formation.assoue.formation.mapper.FormationMapper;
import bf.formation.assoue.formation.model.Formation;
import bf.formation.assoue.formation.model.InscriptionFormation;
import bf.formation.assoue.formation.repository.FormationRepository;
import bf.formation.assoue.formation.repository.InscriptionFormationRepository;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Formations proposees par As'Soue a la population, avec inscription. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormationService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH'h'mm");

    private final FormationRepository formationRepository;
    private final InscriptionFormationRepository inscriptionRepository;
    private final FormationMapper formationMapper;
    private final SmsGatewayService smsGatewayService;
    private final AuthService authService;

    /** Reserve a l'ADMIN. */
    public FormationResponseDTO creer(FormationCreateDTO requete) {
        Formation formation = Formation.builder()
                .titre(requete.getTitre())
                .description(requete.getDescription())
                .lieu(requete.getLieu())
                .dateFormation(requete.getDateFormation())
                .placesDisponibles(requete.getPlacesDisponibles())
                .build();
        formation = formationRepository.save(formation);
        return formationMapper.toDto(formation, 0);
    }

    /** Public : consultable sans etre connecte, comme le catalogue produits. */
    public List<FormationResponseDTO> lister() {
        return formationRepository.findAllByOrderByDateFormationAsc().stream()
                .map(f -> formationMapper.toDto(f, inscriptionRepository.countByFormationId(f.getId())))
                .toList();
    }

    public FormationResponseDTO obtenir(Long id) {
        Formation formation = trouver(id);
        return formationMapper.toDto(formation, inscriptionRepository.countByFormationId(id));
    }

    /** Ouvert a tout utilisateur connecte (CITOYEN, ENTREPRISE ou ADMIN). */
    public InscriptionFormationResponseDTO sInscrire(Long utilisateurId, Long formationId) {
        Formation formation = trouver(formationId);

        if (inscriptionRepository.existsByFormationIdAndUtilisateurId(formationId, utilisateurId)) {
            throw new DejaInscritFormationException(formationId);
        }
        if (formation.getPlacesDisponibles() != null
                && inscriptionRepository.countByFormationId(formationId) >= formation.getPlacesDisponibles()) {
            throw new FormationCompleteException(formationId);
        }

        InscriptionFormation inscription = inscriptionRepository.save(InscriptionFormation.builder()
                .formationId(formationId)
                .utilisateurId(utilisateurId)
                .build());

        try {
            UtilisateurResponseDTO utilisateur = authService.obtenirParId(utilisateurId);
            SmsRequestDTO sms = new SmsRequestDTO();
            sms.setTelephone(utilisateur.getTelephone());
            sms.setType("INSCRIPTION_FORMATION");
            sms.setContenu("Inscription confirmee : \"" + formation.getTitre() + "\" le "
                    + formation.getDateFormation().format(FORMAT_DATE) + " a " + formation.getLieu());
            smsGatewayService.envoyer(sms);
        } catch (Exception e) {
            log.warn("Notification SMS echouee pour l'inscription a la formation {}", formationId, e);
        }

        return InscriptionFormationResponseDTO.builder()
                .id(inscription.getId())
                .formationId(formation.getId())
                .titreFormation(formation.getTitre())
                .dateFormation(formation.getDateFormation())
                .dateInscription(inscription.getDateInscription())
                .build();
    }

    /** Historique des formations auxquelles l'utilisateur connecte s'est inscrit. */
    public List<InscriptionFormationResponseDTO> mesInscriptions(Long utilisateurId) {
        return inscriptionRepository.findByUtilisateurIdOrderByDateInscriptionDesc(utilisateurId).stream()
                .map(inscription -> {
                    Formation formation = trouver(inscription.getFormationId());
                    return InscriptionFormationResponseDTO.builder()
                            .id(inscription.getId())
                            .formationId(formation.getId())
                            .titreFormation(formation.getTitre())
                            .dateFormation(formation.getDateFormation())
                            .dateInscription(inscription.getDateInscription())
                            .build();
                })
                .toList();
    }

    /** Reserve a l'ADMIN : qui s'est inscrit a une formation donnee. */
    public List<UtilisateurResponseDTO> listerInscrits(Long formationId) {
        trouver(formationId); // 404 si la formation n'existe pas
        return inscriptionRepository.findByFormationId(formationId).stream()
                .map(inscription -> authService.obtenirParId(inscription.getUtilisateurId()))
                .toList();
    }

    private Formation trouver(Long id) {
        return formationRepository.findById(id).orElseThrow(() -> new FormationNotFoundException(id));
    }
}
