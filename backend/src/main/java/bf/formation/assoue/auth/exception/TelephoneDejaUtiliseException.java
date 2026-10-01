package bf.formation.assoue.auth.exception;

public class TelephoneDejaUtiliseException extends RuntimeException {
    public TelephoneDejaUtiliseException(String telephone) {
        super("Le numero " + telephone + " est deja associe a un compte");
    }
}
