package bf.formation.assoue.signalement.exception;

public class SignalementNotFoundException extends RuntimeException {
    public SignalementNotFoundException(Long id) {
        super("Signalement introuvable : " + id);
    }
}
