package bf.formation.assoue.paiement.service;

import bf.formation.assoue.common.event.PaiementConfirmeEvent;
import bf.formation.assoue.paiement.dto.InitierPaiementDTO;
import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.exception.TransactionNotFoundException;
import bf.formation.assoue.paiement.mapper.TransactionMapper;
import bf.formation.assoue.common.model.Operateur;
import bf.formation.assoue.paiement.model.StatutTransaction;
import bf.formation.assoue.paiement.model.Transaction;
import bf.formation.assoue.paiement.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * L'ancien CommandeClient (Feign) est remplace par ApplicationEventPublisher :
 * PaiementService.confirmer publie desormais un PaiementConfirmeEvent au lieu
 * d'appeler CommandeService directement (evite un cycle de dependances Spring,
 * cf. CommandeService/PaiementService).
 */
@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    private final TransactionMapper transactionMapper = new TransactionMapper();

    private PaiementService paiementService;

    @BeforeEach
    void setUp() {
        paiementService = new PaiementService(transactionRepository, transactionMapper, eventPublisher);
    }

    @Test
    void initier_shouldCreateTransactionEnInitiee() {
        InitierPaiementDTO requete = new InitierPaiementDTO();
        requete.setCommandeId(7L);
        requete.setOperateur(Operateur.ORANGE_MONEY);
        requete.setMontant(BigDecimal.valueOf(15000));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TransactionResponseDTO result = paiementService.initier(requete);

        assertThat(result.getStatut()).isEqualTo(StatutTransaction.INITIEE);
        assertThat(result.getCommandeId()).isEqualTo(7L);
        assertThat(result.getReferenceExterne()).startsWith("SIM-");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void confirmer_shouldSetStatutAndPublishEvent_whenSucces() {
        Transaction transaction = Transaction.builder()
                .id(1L).commandeId(7L).operateur(Operateur.MOOV_MONEY)
                .montant(BigDecimal.valueOf(15000)).statut(StatutTransaction.INITIEE)
                .referenceExterne("SIM-ABC123").build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDTO result = paiementService.confirmer(1L, true);

        assertThat(result.getStatut()).isEqualTo(StatutTransaction.CONFIRMEE);
        verify(eventPublisher).publishEvent(new PaiementConfirmeEvent(7L, true));
    }

    @Test
    void confirmer_shouldSetEchouee_whenSuccesFalse() {
        Transaction transaction = Transaction.builder()
                .id(1L).commandeId(7L).operateur(Operateur.MOOV_MONEY)
                .montant(BigDecimal.valueOf(15000)).statut(StatutTransaction.INITIEE)
                .referenceExterne("SIM-ABC123").build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDTO result = paiementService.confirmer(1L, false);

        assertThat(result.getStatut()).isEqualTo(StatutTransaction.ECHOUEE);
        verify(eventPublisher).publishEvent(new PaiementConfirmeEvent(7L, false));
    }

    @Test
    void confirmer_shouldThrow_whenTransactionNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paiementService.confirmer(99L, true))
                .isInstanceOf(TransactionNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }
}
