package bf.formation.assoue.auth.repository;

import bf.formation.assoue.auth.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * "telephone" est chiffre de maniere non deterministe en base (cf. EncryptedStringConverter) :
 * toute recherche/verification d'unicite passe desormais par telephoneHash, jamais
 * par le champ telephone lui-meme.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByTelephoneHash(String telephoneHash);
    boolean existsByTelephoneHash(String telephoneHash);
}
