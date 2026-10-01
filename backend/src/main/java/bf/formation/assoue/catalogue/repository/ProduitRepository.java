package bf.formation.assoue.catalogue.repository;

import bf.formation.assoue.catalogue.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitRepository extends JpaRepository<Produit, Long> {
}
