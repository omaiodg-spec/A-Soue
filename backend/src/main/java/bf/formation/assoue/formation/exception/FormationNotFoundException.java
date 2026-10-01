package bf.formation.assoue.formation.exception;

public class FormationNotFoundException extends RuntimeException {
    public FormationNotFoundException(Long id) {
        super("Formation introuvable : " + id);
    }
}
