package bf.formation.assoue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** BF-AUTH-01 */
@Data
public class RegisterRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le numero de telephone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numero de telephone invalide")
    private String telephone;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String motDePasse;
}
