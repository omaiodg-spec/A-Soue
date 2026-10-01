package bf.formation.assoue.auth.exception;

public class EntrepriseNotFoundException extends RuntimeException {
    public EntrepriseNotFoundException(Long id) {
        super("Entreprise introuvable : " + id);
    }
}
