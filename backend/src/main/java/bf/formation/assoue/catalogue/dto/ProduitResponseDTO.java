package bf.formation.assoue.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private String photoUrl;
    private BigDecimal prixFcfa;
    private boolean disponible;
}
