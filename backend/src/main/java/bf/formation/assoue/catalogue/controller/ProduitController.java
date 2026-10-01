package bf.formation.assoue.catalogue.controller;

import bf.formation.assoue.catalogue.dto.ProduitCreateDTO;
import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.service.ProduitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produits")
@RequiredArgsConstructor
@Tag(name = "Catalogue produits")
public class ProduitController {

    private final ProduitService produitService;

    /** BF-SHOP-01 : catalogue public. */
    @GetMapping
    public List<ProduitResponseDTO> lister() {
        return produitService.lister();
    }

    /** Utilise en public (fiche produit) et par service-commande via Feign. */
    @GetMapping("/{id}")
    public ProduitResponseDTO obtenir(@PathVariable Long id) {
        return produitService.obtenir(id);
    }

    /** BF-BO-01 (role ADMIN). */
    @PostMapping
    public ResponseEntity<ProduitResponseDTO> creer(@Valid @RequestBody ProduitCreateDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produitService.creer(requete));
    }

    /** BF-BO-01 (role ADMIN). */
    @PutMapping("/{id}")
    public ProduitResponseDTO modifier(@PathVariable Long id, @Valid @RequestBody ProduitCreateDTO requete) {
        return produitService.modifier(id, requete);
    }

    /** BF-BO-01 (role ADMIN). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        produitService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
