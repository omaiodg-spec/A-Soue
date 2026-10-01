package bf.formation.assoue.paiement.service;

import bf.formation.assoue.common.event.PaiementConfirmeEvent;
import bf.formation.assoue.paiement.dto.InitierPaiementDTO;
import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.exception.TransactionNotFoundException;
import bf.formation.assoue.paiement.mapper.TransactionMapper;
import bf.formation.assoue.paiement.model.StatutTransaction;
import bf.formation.assoue.paiement.model.Transaction;
import bf.formation.assoue.paiement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * BF-SHOP-03 : integration Orange Money / Moov Money.
 * En attendant les cles d'acces fournies par As'Soue (cf. CDC section 3A),
 * les appels operateur sont simules ; seul PaiementService#confirmer
 * changera pour appeler le vrai webhook Orange/Moov.
 */
@Service
@RequiredArgsConstructor
public class PaiementService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Appele directement par CommandeService au moment de valider la commande. */
    public TransactionResponseDTO initier(InitierPaiementDTO requete) {
        Transaction transaction = Transaction.builder()
                .commandeId(requete.getCommandeId())
                .operateur(requete.getOperateur())
                .montant(requete.getMontant())
                .statut(StatutTransaction.INITIEE)
                .referenceExterne("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        return transactionMapper.toDto(transactionRepository.save(transaction));
    }

    /**
     * Simule le webhook de confirmation de l'operateur mobile money.
     * A remplacer par le vrai endpoint webhook Orange/Moov une fois integre.
     */
    public TransactionResponseDTO confirmer(Long transactionId, boolean succes) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        transaction.setStatut(succes ? StatutTransaction.CONFIRMEE : StatutTransaction.ECHOUEE);
        transaction = transactionRepository.save(transaction);

        // Cf. PaiementConfirmeEvent : evite un cycle de dependances avec CommandeService.
        eventPublisher.publishEvent(new PaiementConfirmeEvent(transaction.getCommandeId(), succes));

        return transactionMapper.toDto(transaction);
    }

    public TransactionResponseDTO obtenir(Long id) {
        return transactionMapper.toDto(transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id)));
    }
}

