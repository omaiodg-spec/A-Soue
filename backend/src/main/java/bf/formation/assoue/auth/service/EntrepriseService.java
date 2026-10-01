package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.dto.EntrepriseCreateDTO;
import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.exception.AssoueDejaEnregistreeException;
import bf.formation.assoue.auth.exception.EntrepriseNotFoundException;
import bf.formation.assoue.auth.exception.LocalisationManquanteException;
import bf.formation.assoue.auth.exception.TelephoneDejaUtiliseException;
import bf.formation.assoue.auth.mapper.EntrepriseMapper;
import bf.formation.assoue.auth.model.ProfilEntreprise;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.common.geocoding.dto.CoordonneesDTO;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.ProfilEntrepriseRepository;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/** BF-BO : gestion des comptes entreprise partenaires, reservee a l'ADMIN. */
@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilEntrepriseRepository profilEntrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntrepriseMapper entrepriseMapper;
    private final TelephoneHasher telephoneHasher;
    private final GeocodingService geocodingService;

    /** Le compte est cree directement actif : pas d'auto-inscription, l'ADMIN a deja verifie l'entreprise. */
    public EntrepriseResponseDTO creer(EntrepriseCreateDTO requete) {
        String telephoneHash = telephoneHasher.hacher(requete.getTelephone());
        if (utilisateurRepository.existsByTelephoneHash(telephoneHash)) {
            throw new TelephoneDejaUtiliseException(requete.getTelephone());
        }
        // Un seul compte peut representer As'Soue elle-meme (cf. SignalementService : les
        // signalements PNEU lui sont automatiquement rattaches, pas de mise en concurrence).
        if (requete.isEstAssoue() && profilEntrepriseRepository.existsByEstAssoueTrue()) {
            throw new AssoueDejaEnregistreeException();
        }

        Localisation localisation = resoudreLocalisation(requete);

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(requete.getRaisonSociale())
                .telephone(requete.getTelephone())
                .telephoneHash(telephoneHash)
                .motDePasseHash(passwordEncoder.encode(requete.getMotDePasse()))
                .role(Role.ENTREPRISE)
                .telephoneVerifie(true)
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);

        ProfilEntreprise profil = ProfilEntreprise.builder()
                .utilisateur(utilisateur)
                .raisonSociale(requete.getRaisonSociale())
                .latitude(localisation.latitude())
                .longitude(localisation.longitude())
                .adresse(localisation.adresse())
                .typesDechetGeres(requete.getTypesDechetGeres())
                .estAssoue(requete.isEstAssoue())
                .build();
        profil = profilEntrepriseRepository.save(profil);

        return entrepriseMapper.toDto(profil);
    }

    /**
     * Accepte soit des coordonnees GPS directes, soit une adresse tapee (au moins
     * l'une des deux, cf. EntrepriseCreateDTO). Geocodage via OpenStreetMap/Nominatim
     * dans les deux sens (cf. GeocodingService) :
     * - coordonnees fournies -> geocodage INVERSE (best effort) pour obtenir une
     *   adresse lisible a afficher ;
     * - seulement une adresse fournie -> geocodage DIRECT (obligatoire, sinon
     *   impossible de calculer des distances pour le matching des signalements).
     */
    private Localisation resoudreLocalisation(EntrepriseCreateDTO requete) {
        if (requete.getLatitude() != null && requete.getLongitude() != null) {
            String adresse = requete.getAdresse() != null
                    ? requete.getAdresse()
                    : geocodingService.reverse(requete.getLatitude(), requete.getLongitude()).orElse(null);
            return new Localisation(requete.getLatitude(), requete.getLongitude(), adresse);
        }

        if (requete.getAdresse() != null && !requete.getAdresse().isBlank()) {
            CoordonneesDTO coordonnees = geocodingService.forward(requete.getAdresse());
            return new Localisation(coordonnees.getLatitude(), coordonnees.getLongitude(), requete.getAdresse());
        }

        throw new LocalisationManquanteException();
    }

    private record Localisation(Double latitude, Double longitude, String adresse) {
    }

    public EntrepriseResponseDTO obtenir(Long id) {
        return entrepriseMapper.toDto(trouver(id));
    }

    public List<EntrepriseResponseDTO> lister() {
        return profilEntrepriseRepository.findAll().stream().map(entrepriseMapper::toDto).toList();
    }

    /** Consomme par SignalementService pour trouver les entreprises d'un type donne. */
    public List<EntrepriseResponseDTO> listerParType(TypeDechet type) {
        return profilEntrepriseRepository.findByTypeDechetGere(type).stream()
                .map(entrepriseMapper::toDto)
                .toList();
    }

    /** Utilise par SignalementService pour rattacher automatiquement les signalements PNEU. */
    public java.util.Optional<EntrepriseResponseDTO> obtenirCompteAssoue() {
        return profilEntrepriseRepository.findByEstAssoueTrue().map(entrepriseMapper::toDto);
    }

    private ProfilEntreprise trouver(Long id) {
        return profilEntrepriseRepository.findById(id).orElseThrow(() -> new EntrepriseNotFoundException(id));
    }
}
