package bf.formation.assoue.auth.exception;

/** Un seul compte entreprise peut representer As'Soue elle-meme (cf. ProfilEntreprise.estAssoue). */
public class AssoueDejaEnregistreeException extends RuntimeException {
    public AssoueDejaEnregistreeException() {
        super("Un compte As'Soue est deja enregistre -- une seule entreprise peut porter ce marqueur");
    }
}
