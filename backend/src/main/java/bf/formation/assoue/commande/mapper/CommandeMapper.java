package bf.formation.assoue.commande.mapper;

import bf.formation.assoue.commande.dto.CommandeResponseDTO;
import bf.formation.assoue.commande.dto.LigneResponseDTO;
import bf.formation.assoue.commande.model.Commande;
import org.springframework.stereotype.Component;

@Component
public class CommandeMapper {
    public CommandeResponseDTO toDto(Commande c) {
        return CommandeResponseDTO.builder()
                .id(c.getId())
                .utilisateurId(c.getUtilisateurId())
                .montantTotal(c.getMontantTotal())
                .statut(c.getStatut())
                .dateCreation(c.getDateCreation())
                .lignes(c.getLignes().stream()
                        .map(l -> LigneResponseDTO.builder()
                                .produitId(l.getProduitId())
                                .quantite(l.getQuantite())
                                .prixUnitaire(l.getPrixUnitaire())
                                .build())
                        .toList())
                .build();
    }
}
