package bf.formation.assoue.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuse de demarrer (ou avertit) quand un secret garde sa valeur d'exemple.
 *
 * Les valeurs "CHANGE_MOI..." sont publiques (elles figurent dans le depot et dans
 * application.properties) : quiconque les connait peut forger un JWT ADMIN, signer un
 * faux webhook de paiement ou dechiffrer les donnees personnelles. Avec
 * APP_STRICT_SECRETS=true (defaut du docker-compose), l'application ne demarre pas tant
 * que JWT_SECRET, DATA_ENCRYPTION_KEY, PAYMENT_WEBHOOK_SECRET et ADMIN_DEFAULT_PASSWORD
 * n'ont pas ete remplaces par de vraies valeurs.
 */
@Component
@Slf4j
public class SecretsGuard {

    private static final String MARQUEUR_EXEMPLE = "CHANGE_MOI";
    private static final String ADMIN_PAR_DEFAUT = "admin123";
    private static final int LONGUEUR_MIN_SECRET = 32;
    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 12;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${data.encryption-key}")
    private String cleChiffrement;

    @Value("${payment.webhook-secret}")
    private String secretWebhook;

    @Value("${admin.default-password}")
    private String motDePasseAdmin;

    @Value("${app.strict-secrets:false}")
    private boolean strict;

    @PostConstruct
    void verifier() {
        List<String> problemes = new ArrayList<>();
        controlerSecret("JWT_SECRET", jwtSecret, problemes);
        controlerSecret("DATA_ENCRYPTION_KEY", cleChiffrement, problemes);
        controlerSecret("PAYMENT_WEBHOOK_SECRET", secretWebhook, problemes);
        if (motDePasseAdmin == null || ADMIN_PAR_DEFAUT.equals(motDePasseAdmin)
                || motDePasseAdmin.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            problemes.add("ADMIN_DEFAULT_PASSWORD (valeur par defaut ou < " + LONGUEUR_MIN_MOT_DE_PASSE + " caracteres)");
        }

        if (problemes.isEmpty()) {
            return;
        }
        String message = "Secrets non conformes : " + String.join(" ; ", problemes);
        if (strict) {
            throw new IllegalStateException(message + " -- demarrage refuse (APP_STRICT_SECRETS=true).");
        }
        log.warn("{} -- acceptable en dev local uniquement, jamais en production.", message);
    }

    private void controlerSecret(String nom, String valeur, List<String> problemes) {
        if (valeur == null || valeur.contains(MARQUEUR_EXEMPLE) || valeur.length() < LONGUEUR_MIN_SECRET) {
            problemes.add(nom + " (absent, valeur d'exemple ou < " + LONGUEUR_MIN_SECRET + " caracteres)");
        }
    }
}
