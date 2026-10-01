package bf.formation.assoue.commande.repository;

import bf.formation.assoue.commande.model.Commande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
}
