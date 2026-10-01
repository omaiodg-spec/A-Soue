package bf.formation.assoue.commande.dto;

import bf.formation.assoue.commande.model.StatutCommande;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandeResponseDTO {
    private Long id;
    private Long utilisateurId;
    private BigDecimal montantTotal;
    private StatutCommande statut;
    private List<LigneResponseDTO> lignes;
    private LocalDateTime dateCreation;
}
