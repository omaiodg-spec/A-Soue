package bf.formation.assoue.auth.controller;

import bf.formation.assoue.auth.dto.*;
import bf.formation.assoue.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/inscription")
    public ResponseEntity<UtilisateurResponseDTO> inscrire(@Valid @RequestBody RegisterRequestDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrire(requete));
    }

    @PostMapping("/valider-inscription")
    public ResponseEntity<Void> validerInscription(@Valid @RequestBody OtpVerifyRequestDTO requete) {
        authService.validerInscription(requete);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/connexion")
    public ResponseEntity<JwtResponseDTO> connecter(@Valid @RequestBody LoginRequestDTO requete) {
        return ResponseEntity.ok(authService.connecter(requete));
    }

    /** BNF Securite : deuxieme etape de connexion pour un compte ADMIN (code recu par SMS). */
    @PostMapping("/connexion/verifier-otp")
    public ResponseEntity<JwtResponseDTO> verifierDeuxFacteurs(@Valid @RequestBody OtpVerifyRequestDTO requete) {
        return ResponseEntity.ok(authService.verifierDeuxFacteurs(requete));
    }

    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<Void> demanderReinitialisation(@RequestParam String telephone) {
        authService.demanderReinitialisation(telephone);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<Void> confirmerReinitialisation(@Valid @RequestBody ResetPasswordRequestDTO requete) {
        authService.confirmerReinitialisation(requete);
        return ResponseEntity.ok().build();
    }
}
