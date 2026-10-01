package bf.formation.assoue.formation.exception;

public class FormationCompleteException extends RuntimeException {
    public FormationCompleteException(Long id) {
        super("Formation complete, plus de places disponibles : " + id);
    }
}
