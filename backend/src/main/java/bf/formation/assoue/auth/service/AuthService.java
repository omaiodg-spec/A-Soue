package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.dto.*;
import bf.formation.assoue.auth.exception.IdentifiantsInvalidesException;
import bf.formation.assoue.auth.exception.TelephoneDejaUtiliseException;
import bf.formation.assoue.auth.exception.UtilisateurNotFoundException;
import bf.formation.assoue.auth.mapper.UtilisateurMapper;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.model.TypeOtp;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import bf.formation.assoue.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final UtilisateurMapper utilisateurMapper;
    private final TelephoneHasher telephoneHasher;

    /** BF-AUTH-01 + BF-AUTH-02 : creation du compte puis envoi de l'OTP. */
    public UtilisateurResponseDTO inscrire(RegisterRequestDTO requete) {
        String telephoneHash = telephoneHasher.hacher(requete.getTelephone());
        if (utilisateurRepository.existsByTelephoneHash(telephoneHash)) {
            throw new TelephoneDejaUtiliseException(requete.getTelephone());
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .nom(requete.getNom())
                .telephone(requete.getTelephone())
                .telephoneHash(telephoneHash)
                .motDePasseHash(passwordEncoder.encode(requete.getMotDePasse()))
                .role(Role.CITOYEN)
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);

        otpService.genererEtEnvoyer(requete.getTelephone(), TypeOtp.VALIDATION_INSCRIPTION);
        return utilisateurMapper.toDto(utilisateur);
    }

    /** BF-AUTH-02 : validation du numero via le code recu par SMS. */
    public void validerInscription(OtpVerifyRequestDTO requete) {
        otpService.verifier(requete.getTelephone(), requete.getCode(), TypeOtp.VALIDATION_INSCRIPTION);
        Utilisateur utilisateur = utilisateurRepository.findByTelephoneHash(telephoneHasher.hacher(requete.getTelephone()))
                .orElseThrow(() -> new UtilisateurNotFoundException("Utilisateur introuvable"));
        utilisateur.setTelephoneVerifie(true);
        utilisateurRepository.save(utilisateur);
    }

    /** BF-AUTH-01 + BF-AUTH-03 : connexion citoyen ou administrateur. */
    public JwtResponseDTO connecter(LoginRequestDTO requete) {
        Utilisateur utilisateur = utilisateurRepository.findByTelephoneHash(telephoneHasher.hacher(requete.getTelephone()))
                .orElseThrow(() -> new IdentifiantsInvalidesException("Telephone ou mot de passe incorrect"));

        if (!passwordEncoder.matches(requete.getMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw new IdentifiantsInvalidesException("Telephone ou mot de passe incorrect");
        }
        if (!utilisateur.isTelephoneVerifie()) {
            throw new IdentifiantsInvalidesException("Le numero de telephone n'a pas encore ete valide par OTP");
        }

        // BNF Securite (CDC section 4) : 2FA par SMS obligatoire pour les comptes ADMIN
        // (teste en penetration au jalon 2). Le mot de passe seul ne suffit pas :
        // on envoie un code et on ne rend PAS le JWT tant qu'il n'est pas confirme
        // via /auth/connexion/verifier-otp.
        if (utilisateur.getRole() == Role.ADMIN) {
            otpService.genererEtEnvoyer(utilisateur.getTelephone(), TypeOtp.DEUX_FACTEURS_ADMIN);
            return JwtResponseDTO.builder()
                    .otpRequis(true)
                    .userId(utilisateur.getId())
                    .role(utilisateur.getRole().name())
                    .build();
        }

        return construireReponseJwt(utilisateur);
    }

    /** BNF Securite : deuxieme etape de la connexion ADMIN, verification du code SMS. */
    public JwtResponseDTO verifierDeuxFacteurs(OtpVerifyRequestDTO requete) {
        Utilisateur utilisateur = utilisateurRepository.findByTelephoneHash(telephoneHasher.hacher(requete.getTelephone()))
                .orElseThrow(() -> new UtilisateurNotFoundException("Utilisateur introuvable"));
        if (utilisateur.getRole() != Role.ADMIN) {
            throw new IdentifiantsInvalidesException("Le 2FA n'est requis que pour les comptes administrateur");
        }
        otpService.verifier(requete.getTelephone(), requete.getCode(), TypeOtp.DEUX_FACTEURS_ADMIN);
        return construireReponseJwt(utilisateur);
    }

    private JwtResponseDTO construireReponseJwt(Utilisateur utilisateur) {
        String token = jwtService.generateToken(utilisateur.getId(), utilisateur.getTelephone(),
                utilisateur.getRole().name());
        return JwtResponseDTO.builder()
                .token(token)
                .userId(utilisateur.getId())
                .role(utilisateur.getRole().name())
                .build();
    }

    /** BF-AUTH-04 : demande de reinitialisation (envoi de l'OTP). */
    public void demanderReinitialisation(String telephone) {
        if (!utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher(telephone))) {
            throw new UtilisateurNotFoundException("Aucun compte associe a ce numero");
        }
        otpService.genererEtEnvoyer(telephone, TypeOtp.REINITIALISATION_MDP);
    }

    /** BF-AUTH-04 : confirmation de la reinitialisation. */
    public void confirmerReinitialisation(ResetPasswordRequestDTO requete) {
        otpService.verifier(requete.getTelephone(), requete.getCode(), TypeOtp.REINITIALISATION_MDP);
        Utilisateur utilisateur = utilisateurRepository.findByTelephoneHash(telephoneHasher.hacher(requete.getTelephone()))
                .orElseThrow(() -> new UtilisateurNotFoundException("Utilisateur introuvable"));
        utilisateur.setMotDePasseHash(passwordEncoder.encode(requete.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
    }

    /** Endpoint interne utilise par les autres microservices (pattern Feign, cf. taskboard). */
    public UtilisateurResponseDTO obtenirParId(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurNotFoundException("Utilisateur introuvable : " + id));
        return utilisateurMapper.toDto(utilisateur);
    }

    /**
     * Mise a jour du compte par son titulaire (nom / telephone / mot de
     * passe), une fois connecte -- utile notamment pour que l'administrateur
     * seede par defaut (cf. AdminSeeder) change son mot de passe initial.
     * Seuls les champs non vides de la requete sont modifies.
     */
    public UtilisateurResponseDTO mettreAJourCompte(Long utilisateurId, MettreAJourCompteDTO requete) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new UtilisateurNotFoundException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(requete.getMotDePasseActuel(), utilisateur.getMotDePasseHash())) {
            throw new IdentifiantsInvalidesException("Mot de passe actuel incorrect");
        }

        if (requete.getNom() != null && !requete.getNom().isBlank()) {
            utilisateur.setNom(requete.getNom());
        }

        if (requete.getTelephone() != null && !requete.getTelephone().isBlank()
                && !requete.getTelephone().equals(utilisateur.getTelephone())) {
            String nouveauHash = telephoneHasher.hacher(requete.getTelephone());
            if (utilisateurRepository.existsByTelephoneHash(nouveauHash)) {
                throw new TelephoneDejaUtiliseException(requete.getTelephone());
            }
            utilisateur.setTelephone(requete.getTelephone());
            utilisateur.setTelephoneHash(nouveauHash);
        }

        if (requete.getNouveauMotDePasse() != null && !requete.getNouveauMotDePasse().isBlank()) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(requete.getNouveauMotDePasse()));
        }

        utilisateur = utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toDto(utilisateur);
    }
}
