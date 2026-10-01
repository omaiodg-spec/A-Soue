package bf.formation.assoue.catalogue.exception;

public class ProduitNotFoundException extends RuntimeException {
    public ProduitNotFoundException(Long id) {
        super("Produit introuvable : " + id);
    }
}
