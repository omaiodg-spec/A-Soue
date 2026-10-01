package bf.formation.assoue.commande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneResponseDTO {
    private Long produitId;
    private int quantite;
    private BigDecimal prixUnitaire;
}
