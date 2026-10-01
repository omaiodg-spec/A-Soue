package bf.formation.assoue.paiement.dto;

import bf.formation.assoue.common.model.Operateur;
import bf.formation.assoue.paiement.model.StatutTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private Long id;
    private Long commandeId;
    private Operateur operateur;
    private BigDecimal montant;
    private StatutTransaction statut;
    private String referenceExterne;
}
