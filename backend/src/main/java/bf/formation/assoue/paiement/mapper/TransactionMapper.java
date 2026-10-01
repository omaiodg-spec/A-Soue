package bf.formation.assoue.paiement.mapper;

import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponseDTO toDto(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .commandeId(t.getCommandeId())
                .operateur(t.getOperateur())
                .montant(t.getMontant())
                .statut(t.getStatut())
                .referenceExterne(t.getReferenceExterne())
                .build();
    }
}
