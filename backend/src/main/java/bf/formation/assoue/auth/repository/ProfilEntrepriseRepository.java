package bf.formation.assoue.auth.repository;

import bf.formation.assoue.auth.model.ProfilEntreprise;
import bf.formation.assoue.common.model.TypeDechet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfilEntrepriseRepository extends JpaRepository<ProfilEntreprise, Long> {

    @Query("select p from ProfilEntreprise p join p.typesDechetGeres t where t = :type")
    List<ProfilEntreprise> findByTypeDechetGere(@Param("type") TypeDechet type);

    java.util.Optional<ProfilEntreprise> findByEstAssoueTrue();

    boolean existsByEstAssoueTrue();
}
