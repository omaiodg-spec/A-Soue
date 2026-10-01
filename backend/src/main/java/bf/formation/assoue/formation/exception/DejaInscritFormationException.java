package bf.formation.assoue.formation.exception;

public class DejaInscritFormationException extends RuntimeException {
    public DejaInscritFormationException(Long formationId) {
        super("Deja inscrit a cette formation : " + formationId);
    }
}
