package bf.formation.assoue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank
    private String telephone;
    @NotBlank
    private String motDePasse;
}
