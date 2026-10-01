package bf.formation.assoue.auth.controller;

import bf.formation.assoue.auth.dto.MettreAJourCompteDTO;
import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.auth.service.AuthService;
import bf.formation.assoue.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Compte de l'utilisateur connecte (quel que soit son role). Protege par
 * SecurityConfig via la regle generale .anyRequest().authenticated() --
 * "/utilisateurs/**" n'est pas dans la liste des routes publiques.
 */
@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @GetMapping("/moi")
    public UtilisateurResponseDTO monCompte() {
        return authService.obtenirParId(currentUser.id());
    }

    /** Permet notamment a l'administrateur seede par defaut (cf. AdminSeeder) de changer son mot de passe initial. */
    @PutMapping("/moi")
    public UtilisateurResponseDTO mettreAJourMonCompte(@Valid @RequestBody MettreAJourCompteDTO requete) {
        return authService.mettreAJourCompte(currentUser.id(), requete);
    }
}
