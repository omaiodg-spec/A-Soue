package bf.formation.assoue.signalement.repository;

import bf.formation.assoue.signalement.model.Signalement;
import bf.formation.assoue.signalement.model.StatutSignalement;
import bf.formation.assoue.common.model.TypeDechet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalementRepository extends JpaRepository<Signalement, Long> {
    List<Signalement> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

    // Fil "marketplace" d'une entreprise : signalements de son type pas encore pris.
    List<Signalement> findByTypeDechetAndStatut(TypeDechet typeDechet, StatutSignalement statut);

    // Signalements automatiquement attribues au compte interne As'Soue.
    List<Signalement> findByEntrepriseIdAndStatut(Long entrepriseId, StatutSignalement statut);
}
