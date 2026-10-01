package bf.formation.assoue.commande.dto;

import bf.formation.assoue.commande.model.StatutCommande;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatutCommandeUpdateDTO {
    @NotNull
    private StatutCommande statut;
}
