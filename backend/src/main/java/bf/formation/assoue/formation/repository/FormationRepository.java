package bf.formation.assoue.formation.repository;

import bf.formation.assoue.formation.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormationRepository extends JpaRepository<Formation, Long> {
    List<Formation> findAllByOrderByDateFormationAsc();
}
