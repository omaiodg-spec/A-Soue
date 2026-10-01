package bf.formation.assoue.auth.config;

import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cree un compte ADMIN par defaut au demarrage si aucun n'existe encore avec
 * cet identifiant -- pratique pour un premier acces sans devoir bidouiller
 * la base a la main (cf. discussion "comment l'admin se connecte").
 *
 * Le "telephone" sert d'identifiant de connexion dans tout le systeme (voir
 * Utilisateur.java) ; on y met ici "admin@cif.bf" -- ce n'est pas un vrai
 * numero, mais rien cote backend n'impose ce format pour la connexion
 * (LoginRequestDTO n'a pas de @Pattern, contrairement a l'inscription
 * publique). Compte deja marque telephoneVerifie=true pour pouvoir se
 * connecter immediatement.
 *
 * IMPORTANT : identifiants par defaut connus de tous -- a changer via
 * PUT /utilisateurs/moi des la premiere connexion (page "Mon compte" cote
 * frontend). Ce seeder ne re-ecrase jamais un compte existant : une fois le
 * mot de passe change, il n'est plus reinitialise au redemarrage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private static final String TELEPHONE_ADMIN_DEFAUT = "admin@cif.bf";
    private static final String MOT_DE_PASSE_ADMIN_DEFAUT = "admin123";

    private final UtilisateurRepository utilisateurRepository;
    private final TelephoneHasher telephoneHasher;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String hash = telephoneHasher.hacher(TELEPHONE_ADMIN_DEFAUT);
        if (utilisateurRepository.existsByTelephoneHash(hash)) {
            return; // deja cree (ou identifiant change depuis) : on ne touche a rien.
        }

        Utilisateur admin = Utilisateur.builder()
                .nom("Administrateur CIF")
                .telephone(TELEPHONE_ADMIN_DEFAUT)
                .telephoneHash(hash)
                .motDePasseHash(passwordEncoder.encode(MOT_DE_PASSE_ADMIN_DEFAUT))
                .role(Role.ADMIN)
                .telephoneVerifie(true)
                .build();
        utilisateurRepository.save(admin);

        log.warn("Compte administrateur par defaut cree : {} / {} -- a changer via 'Mon compte' apres la premiere connexion !",
                TELEPHONE_ADMIN_DEFAUT, MOT_DE_PASSE_ADMIN_DEFAUT);
    }
}
