package bf.formation.assoue.paiement.controller;

import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.dto.WebhookPayloadDTO;
import bf.formation.assoue.paiement.security.WebhookSignatureVerifier;
import bf.formation.assoue.paiement.service.PaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paiements")
@RequiredArgsConstructor
@Tag(name = "Paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final WebhookSignatureVerifier webhookSignatureVerifier;
    private final ObjectMapper objectMapper;

    /**
     * Webhook operateur (Orange Money / Moov Money) confirmant la transaction.
     * Simule encore le resultat en attendant les cles API fournies par As'Soue,
     * mais la requete DOIT desormais porter une signature HMAC-SHA256 valide
     * (en-tete X-Webhook-Signature, calculee sur le corps brut) : sans elle,
     * n'importe qui pouvait auparavant confirmer une transaction sans avoir paye.
     */
    @SneakyThrows
    @PostMapping("/webhook")
    public ResponseEntity<TransactionResponseDTO> webhook(
            @RequestBody byte[] corpsBrut,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {

        webhookSignatureVerifier.verifier(corpsBrut, signature);

        WebhookPayloadDTO payload = objectMapper.readValue(corpsBrut, WebhookPayloadDTO.class);
        return ResponseEntity.ok(paiementService.confirmer(payload.getTransactionId(), payload.isSucces()));
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO obtenir(@PathVariable Long id) {
        return paiementService.obtenir(id);
    }
}
