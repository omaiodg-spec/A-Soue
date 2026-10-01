package bf.formation.assoue.paiement.dto;

import bf.formation.assoue.common.model.Operateur;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Appele par service-commande (Feign) pour demarrer un paiement. */
@Data
public class InitierPaiementDTO {
    @NotNull
    private Long commandeId;
    @NotNull
    private Operateur operateur;
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal montant;
}
