package bf.formation.assoue.auth.exception;

/** Ni coordonnees GPS ni adresse fournies a la creation d'une entreprise -- au moins l'un des deux est requis. */
public class LocalisationManquanteException extends RuntimeException {
    public LocalisationManquanteException() {
        super("Fournir latitude+longitude ou une adresse pour localiser l'entreprise");
    }
}
