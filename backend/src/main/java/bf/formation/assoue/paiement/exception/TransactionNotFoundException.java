package bf.formation.assoue.paiement.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id) {
        super("Transaction introuvable : " + id);
    }
}
