package bf.formation.assoue.formation.controller;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.common.security.CurrentUser;
import bf.formation.assoue.formation.dto.FormationCreateDTO;
import bf.formation.assoue.formation.dto.FormationResponseDTO;
import bf.formation.assoue.formation.dto.InscriptionFormationResponseDTO;
import bf.formation.assoue.formation.service.FormationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Formations proposees par As'Soue a la population (tri des dechets, recyclage, etc.). */
@RestController
@RequestMapping("/formations")
@RequiredArgsConstructor
@Tag(name = "Formations")
public class FormationController {

    private final FormationService formationService;
    private final CurrentUser currentUser;

    /** Public, comme le catalogue produits : consultable sans etre connecte. */
    @GetMapping
    public List<FormationResponseDTO> lister() {
        return formationService.lister();
    }

    @GetMapping("/{id}")
    public FormationResponseDTO obtenir(@PathVariable Long id) {
        return formationService.obtenir(id);
    }

    /** Reserve a l'ADMIN. */
    @PostMapping
    public ResponseEntity<FormationResponseDTO> creer(@Valid @RequestBody FormationCreateDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.creer(requete));
    }

    /** Ouvert a tout utilisateur connecte (CITOYEN, ENTREPRISE ou ADMIN). */
    @PostMapping("/{id}/inscription")
    public ResponseEntity<InscriptionFormationResponseDTO> sInscrire(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.sInscrire(currentUser.id(), id));
    }

    /** Historique des inscriptions de l'utilisateur connecte. */
    @GetMapping("/mes-inscriptions")
    public List<InscriptionFormationResponseDTO> mesInscriptions() {
        return formationService.mesInscriptions(currentUser.id());
    }

    /** Reserve a l'ADMIN : liste des inscrits a une formation. */
    @GetMapping("/{id}/inscrits")
    public List<UtilisateurResponseDTO> listerInscrits(@PathVariable Long id) {
        return formationService.listerInscrits(id);
    }
}
