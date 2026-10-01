package bf.formation.assoue.auth.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement/dechiffrement AES-256-GCM pour les donnees personnelles au
 * repos (nom, telephone) exigees chiffrees par la CIL Burkina Faso (CDC
 * section 3B). IV aleatoire a chaque appel -> chiffrement NON deterministe
 * (deux chiffrements du meme texte donnent des resultats differents), donc
 * inutilisable tel quel pour une recherche ou une contrainte d'unicite
 * (voir TelephoneHasher pour l'index de recherche associe).
 */
final class AesGcmUtil {

    private static final int GCM_IV_LENGTH_OCTETS = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcmUtil() {
    }

    static String chiffrer(String texteClair, String cleBrute) {
        if (texteClair == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_OCTETS];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriverCle(cleBrute), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] chiffre = cipher.doFinal(texteClair.getBytes(StandardCharsets.UTF_8));

            ByteBuffer tampon = ByteBuffer.allocate(iv.length + chiffre.length);
            tampon.put(iv).put(chiffre);
            return Base64.getEncoder().encodeToString(tampon.array());
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de chiffrement d'une donnee personnelle", e);
        }
    }

    static String dechiffrer(String base64, String cleBrute) {
        if (base64 == null) {
            return null;
        }
        try {
            byte[] donnees = Base64.getDecoder().decode(base64);
            ByteBuffer tampon = ByteBuffer.wrap(donnees);
            byte[] iv = new byte[GCM_IV_LENGTH_OCTETS];
            tampon.get(iv);
            byte[] chiffre = new byte[tampon.remaining()];
            tampon.get(chiffre);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriverCle(cleBrute), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(chiffre), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de dechiffrement d'une donnee personnelle", e);
        }
    }

    /** Derive une cle AES-256 (32 octets) a partir du secret configure, quelle que soit sa longueur. */
    private static SecretKey deriverCle(String cleBrute) throws Exception {
        byte[] cle32 = MessageDigest.getInstance("SHA-256").digest(cleBrute.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(cle32, "AES");
    }
}
