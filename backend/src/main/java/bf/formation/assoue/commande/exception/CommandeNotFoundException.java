package bf.formation.assoue.commande.exception;

public class CommandeNotFoundException extends RuntimeException {
    public CommandeNotFoundException(Long id) {
        super("Commande introuvable : " + id);
    }
}
