package bf.formation.assoue.paiement.exception;

/**
 * Levee quand le webhook operateur (Orange Money / Moov Money) arrive sans
 * signature valide. Avant ce correctif, /paiements/webhook etait un simple
 * permitAll sans aucune verification : n'importe qui pouvait confirmer une
 * transaction comme payee sans avoir paye.
 */
public class SignatureWebhookInvalideException extends RuntimeException {
    public SignatureWebhookInvalideException(String message) {
        super(message);
    }
}
