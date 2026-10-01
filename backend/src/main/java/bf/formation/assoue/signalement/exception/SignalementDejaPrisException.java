package bf.formation.assoue.signalement.exception;

public class SignalementDejaPrisException extends RuntimeException {
    public SignalementDejaPrisException(Long id) {
        super("Ce signalement a deja ete pris en charge par une autre entreprise : " + id);
    }
}
