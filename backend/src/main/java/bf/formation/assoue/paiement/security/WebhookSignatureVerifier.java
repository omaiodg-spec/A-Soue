package bf.formation.assoue.paiement.security;

import bf.formation.assoue.paiement.exception.SignatureWebhookInvalideException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifie la signature HMAC-SHA256 du webhook operateur (Orange Money / Moov Money).
 *
 * Tant que /paiements/webhook simulait la confirmation, il etait en permitAll
 * sans aucune verification : n'importe qui pouvait appeler l'URL et marquer
 * une transaction comme payee. Orange Money et Moov Money signent tous les
 * deux leurs callbacks (cle secrete fournie a l'integration, cf. CDC 3A) ;
 * cette classe verifie cette signature sur le corps brut de la requete
 * AVANT que le JSON ne soit interprete.
 *
 * A l'integration reelle : recuperer la cle secrete de signature aupres de
 * chaque operateur et l'injecter via PAYMENT_WEBHOOK_SECRET (une cle par
 * operateur si Orange et Moov utilisent des schemas differents).
 */
@Component
public class WebhookSignatureVerifier {

    @Value("${payment.webhook-secret}")
    private String secret;

    public void verifier(byte[] corpsBrut, String signatureRecue) {
        if (signatureRecue == null || signatureRecue.isBlank()) {
            throw new SignatureWebhookInvalideException("Signature de webhook manquante (en-tete X-Webhook-Signature)");
        }

        String signatureAttendue = calculerHmacSha256(corpsBrut);

        // Comparaison en temps constant pour eviter les attaques par timing.
        boolean valide = MessageDigest.isEqual(
                signatureAttendue.getBytes(StandardCharsets.UTF_8),
                signatureRecue.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));

        if (!valide) {
            throw new SignatureWebhookInvalideException("Signature de webhook invalide");
        }
    }

    private String calculerHmacSha256(byte[] corpsBrut) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(corpsBrut);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer la signature HMAC du webhook", e);
        }
    }
}
