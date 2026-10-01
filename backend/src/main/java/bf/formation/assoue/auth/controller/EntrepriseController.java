package bf.formation.assoue.auth.controller;

import bf.formation.assoue.auth.dto.EntrepriseCreateDTO;
import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.service.EntrepriseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET /par-type et GET /{id} existaient uniquement pour les appels internes
 * (Feign) de service-signalement ; supprimes avec la fusion, SignalementService
 * appelle desormais EntrepriseService directement (injection Spring). Ces deux
 * routes exposaient nom/telephone d'une entreprise sans authentification.
 */
@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
@Tag(name = "Entreprises partenaires")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    /** BF-BO : creation reservee a l'ADMIN (cf. SecurityConfig). */
    @PostMapping
    public ResponseEntity<EntrepriseResponseDTO> creer(@Valid @RequestBody EntrepriseCreateDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entrepriseService.creer(requete));
    }

    /** BF-BO : liste complete, reservee a l'ADMIN. */
    @GetMapping
    public List<EntrepriseResponseDTO> lister() {
        return entrepriseService.lister();
    }
}
