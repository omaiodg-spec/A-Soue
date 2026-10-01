package bf.formation.assoue.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeOtp type;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Builder.Default
    private boolean utilise = false;

    // Anti brute-force : le code (6 chiffres) est bloque au bout de MAX_TENTATIVES
    // essais incorrects, cf. OtpService#verifier.
    @Builder.Default
    private int tentatives = 0;
}
