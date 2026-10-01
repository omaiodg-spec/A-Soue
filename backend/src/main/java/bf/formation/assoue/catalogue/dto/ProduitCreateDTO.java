package bf.formation.assoue.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** BF-BO-01 : ajout d'un produit par l'administrateur. */
@Data
public class ProduitCreateDTO {
    @NotBlank
    private String titre;
    private String description;
    private String photoUrl;
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal prixFcfa;
    private boolean disponible = true;
}
