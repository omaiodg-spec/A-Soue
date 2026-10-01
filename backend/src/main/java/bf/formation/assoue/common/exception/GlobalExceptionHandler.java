package bf.formation.assoue.common.exception;

import bf.formation.assoue.auth.exception.AssoueDejaEnregistreeException;
import bf.formation.assoue.auth.exception.EntrepriseNotFoundException;
import bf.formation.assoue.auth.exception.IdentifiantsInvalidesException;
import bf.formation.assoue.auth.exception.LocalisationManquanteException;
import bf.formation.assoue.auth.exception.OtpInvalideException;
import bf.formation.assoue.auth.exception.TelephoneDejaUtiliseException;
import bf.formation.assoue.auth.exception.UtilisateurNotFoundException;
import bf.formation.assoue.catalogue.exception.ProduitNotFoundException;
import bf.formation.assoue.commande.exception.CommandeNotFoundException;
import bf.formation.assoue.commande.exception.ProduitIndisponibleException;
import bf.formation.assoue.common.geocoding.exception.GeocodageIndisponibleException;
import bf.formation.assoue.formation.exception.DejaInscritFormationException;
import bf.formation.assoue.formation.exception.FormationCompleteException;
import bf.formation.assoue.formation.exception.FormationNotFoundException;
import bf.formation.assoue.paiement.exception.SignatureWebhookInvalideException;
import bf.formation.assoue.paiement.exception.TransactionNotFoundException;
import bf.formation.assoue.signalement.exception.AccesRefuseException;
import bf.formation.assoue.signalement.exception.PhotoInvalideException;
import bf.formation.assoue.signalement.exception.SignalementDejaPrisException;
import bf.formation.assoue.signalement.exception.SignalementNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions unique pour toute l'application.
 *
 * Avant la fusion, chaque microservice avait son propre @RestControllerAdvice,
 * chacun avec son propre handler pour MethodArgumentNotValidException. Un
 * @RestControllerAdvice s'applique a TOUTE l'application Spring (pas
 * seulement a son package) : en garder 5 separes aurait fait planter le
 * demarrage avec "Ambiguous @ExceptionHandler method mapped for
 * MethodArgumentNotValidException". D'ou la fusion en une seule classe.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- auth ---
    @ExceptionHandler(TelephoneDejaUtiliseException.class)
    public ResponseEntity<ErrorResponse> handleTelephoneDejaUtilise(TelephoneDejaUtiliseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UtilisateurNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUtilisateurNotFound(UtilisateurNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EntrepriseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntrepriseNotFound(EntrepriseNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AssoueDejaEnregistreeException.class)
    public ResponseEntity<ErrorResponse> handleAssoueDejaEnregistree(AssoueDejaEnregistreeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(LocalisationManquanteException.class)
    public ResponseEntity<ErrorResponse> handleLocalisationManquante(LocalisationManquanteException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OtpInvalideException.class)
    public ResponseEntity<ErrorResponse> handleOtpInvalide(OtpInvalideException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErrorResponse> handleIdentifiantsInvalides(IdentifiantsInvalidesException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // --- catalogue ---
    @ExceptionHandler(ProduitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProduitNotFound(ProduitNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- commande ---
    @ExceptionHandler(CommandeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommandeNotFound(CommandeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProduitIndisponibleException.class)
    public ResponseEntity<ErrorResponse> handleProduitIndisponible(ProduitIndisponibleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // --- formation ---
    @ExceptionHandler(FormationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFormationNotFound(FormationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FormationCompleteException.class)
    public ResponseEntity<ErrorResponse> handleFormationComplete(FormationCompleteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DejaInscritFormationException.class)
    public ResponseEntity<ErrorResponse> handleDejaInscrit(DejaInscritFormationException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // --- paiement ---
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SignatureWebhookInvalideException.class)
    public ResponseEntity<ErrorResponse> handleSignatureInvalide(SignatureWebhookInvalideException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // --- signalement ---
    @ExceptionHandler(SignalementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSignalementNotFound(SignalementNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SignalementDejaPrisException.class)
    public ResponseEntity<ErrorResponse> handleDejaPris(SignalementDejaPrisException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<ErrorResponse> handleAccesRefuse(AccesRefuseException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(PhotoInvalideException.class)
    public ResponseEntity<ErrorResponse> handlePhotoInvalide(PhotoInvalideException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(GeocodageIndisponibleException.class)
    public ResponseEntity<ErrorResponse> handleGeocodageIndisponible(GeocodageIndisponibleException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- commun : erreurs de validation @Valid ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder().status(status.value()).message(message).timestamp(LocalDateTime.now()).build());
    }
}
