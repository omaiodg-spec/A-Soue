package bf.formation.assoue.commande.exception;

public class ProduitIndisponibleException extends RuntimeException {
    public ProduitIndisponibleException(Long produitId) {
        super("Produit indisponible ou introuvable : " + produitId);
    }
}
