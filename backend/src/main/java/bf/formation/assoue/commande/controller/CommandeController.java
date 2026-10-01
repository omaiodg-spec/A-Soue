package bf.formation.assoue.commande.controller;

import bf.formation.assoue.commande.dto.CommandeCreateDTO;
import bf.formation.assoue.commande.dto.CommandeResponseDTO;
import bf.formation.assoue.commande.dto.StatutCommandeUpdateDTO;
import bf.formation.assoue.common.security.CurrentUser;
import bf.formation.assoue.commande.service.CommandeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/commandes")
@RequiredArgsConstructor
@Tag(name = "Commandes")
public class CommandeController {

    private final CommandeService commandeService;
    private final CurrentUser currentUser;

    /** BF-SHOP-02 + BF-SHOP-03 (role CITOYEN). */
    @PostMapping
    public ResponseEntity<CommandeResponseDTO> creer(@Valid @RequestBody CommandeCreateDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandeService.creer(currentUser.id(), requete));
    }

    /** BF-SHOP-04 : suivi du citoyen connecte. */
    @GetMapping("/mes-commandes")
    public List<CommandeResponseDTO> mesCommandes() {
        return commandeService.mesCommandes(currentUser.id());
    }

    /** BF-BO (role ADMIN). */
    @GetMapping("/admin")
    public List<CommandeResponseDTO> toutes() {
        return commandeService.toutes();
    }

    /** BF-SHOP-05 (role ADMIN). */
    @PatchMapping("/admin/{id}/statut")
    public CommandeResponseDTO changerStatut(@PathVariable Long id, @Valid @RequestBody StatutCommandeUpdateDTO requete) {
        return commandeService.changerStatut(id, requete.getStatut());
    }

    /** BF-BO-03 : export CSV des commandes (role ADMIN). */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exporter() {
        List<CommandeResponseDTO> commandes = commandeService.toutes();
        String header = "id;utilisateurId;montantTotal;statut;dateCreation\n";
        String lignes = commandes.stream()
                .map(c -> String.join(";",
                        String.valueOf(c.getId()),
                        String.valueOf(c.getUtilisateurId()),
                        String.valueOf(c.getMontantTotal()),
                        c.getStatut().name(),
                        String.valueOf(c.getDateCreation())))
                .collect(Collectors.joining("\n"));

        byte[] csv = (header + lignes).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=commandes.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
