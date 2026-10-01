package bf.formation.assoue.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {
    // null tant que le 2FA (comptes ADMIN) n'est pas valide -- cf. otpRequis.
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String role;
    // BNF Securite (CDC section 4) : true pour un compte ADMIN qui vient de saisir
    // le bon mot de passe mais doit encore fournir le code recu par SMS.
    @Builder.Default
    private boolean otpRequis = false;
}
