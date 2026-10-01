package bf.formation.assoue.common.geocoding.exception;

/** Leve quand Nominatim ne renvoie aucun resultat exploitable (adresse introuvable, service en panne). */
public class GeocodageIndisponibleException extends RuntimeException {
    public GeocodageIndisponibleException(String message) {
        super(message);
    }
}
