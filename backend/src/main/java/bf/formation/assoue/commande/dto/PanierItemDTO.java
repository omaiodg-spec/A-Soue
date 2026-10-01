package bf.formation.assoue.commande.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PanierItemDTO {
    @NotNull
    private Long produitId;
    @Min(1)
    private int quantite;
}
