package bf.formation.assoue.paiement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Corps JSON attendu du webhook operateur, verifie par signature HMAC avant lecture. */
@Data
public class WebhookPayloadDTO {
    @NotNull
    private Long transactionId;
    private boolean succes;
}
