package bf.formation.assoue.auth.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Le champ "telephone" est desormais chiffre de maniere NON deterministe
 * (EncryptedStringConverter, IV aleatoire) : deux chiffrements du meme
 * numero donnent des valeurs differentes en base, donc plus utilisables
 * pour un "WHERE telephone = ?" ni pour garantir l'unicite.
 *
 * Cette classe calcule un HMAC-SHA256 deterministe du numero (meme entree
 * -> meme sortie, toujours) stocke dans une colonne separee (telephone_hash,
 * contrainte unique) : c'est elle qui sert aux recherches et a l'unicite,
 * jamais le texte en clair.
 */
@Component
public class TelephoneHasher {

    @Value("${data.encryption-key}")
    private String cle;

    public String hacher(String telephone) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(cle.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(telephone.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de hachage du numero de telephone", e);
        }
    }
}
