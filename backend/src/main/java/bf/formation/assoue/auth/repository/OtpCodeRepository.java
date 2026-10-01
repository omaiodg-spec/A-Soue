package bf.formation.assoue.auth.repository;

import bf.formation.assoue.auth.model.OtpCode;
import bf.formation.assoue.auth.model.TypeOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc(String telephone, TypeOtp type);
}
