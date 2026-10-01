package bf.formation.assoue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** BF-AUTH-04 : reinitialisation du mot de passe via SMS. */
@Data
public class ResetPasswordRequestDTO {
    @NotBlank
    private String telephone;
    @NotBlank
    private String code;
    @NotBlank
    @Size(min = 8)
    private String nouveauMotDePasse;
}
