package bf.formation.assoue.formation.repository;

import bf.formation.assoue.formation.model.InscriptionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionFormationRepository extends JpaRepository<InscriptionFormation, Long> {
    boolean existsByFormationIdAndUtilisateurId(Long formationId, Long utilisateurId);
    long countByFormationId(Long formationId);
    List<InscriptionFormation> findByUtilisateurIdOrderByDateInscriptionDesc(Long utilisateurId);
    List<InscriptionFormation> findByFormationId(Long formationId);
}
