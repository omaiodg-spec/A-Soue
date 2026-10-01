package bf.formation.assoue.commande.dto;

import bf.formation.assoue.common.model.Operateur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** BF-SHOP-02 + BF-SHOP-03 : validation du panier et choix de l'operateur mobile money. */
@Data
public class CommandeCreateDTO {
    @NotEmpty(message = "Le panier ne peut pas etre vide")
    @Valid
    private List<PanierItemDTO> lignes;

    @NotNull
    private Operateur operateur;
}
