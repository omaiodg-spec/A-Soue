package bf.formation.assoue.signalement.controller;

import bf.formation.assoue.signalement.dto.SignalementCreateDTO;
import bf.formation.assoue.signalement.dto.SignalementMarketplaceDTO;
import bf.formation.assoue.signalement.dto.SignalementResponseDTO;
import bf.formation.assoue.signalement.dto.StatutUpdateDTO;
import bf.formation.assoue.common.security.CurrentUser;
import bf.formation.assoue.signalement.service.PhotoStorageService;
import bf.formation.assoue.signalement.service.SignalementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/signalements")
@RequiredArgsConstructor
@Tag(name = "Signalements")
public class SignalementController {

    private final SignalementService signalementService;
    private final PhotoStorageService photoStorageService;
    private final CurrentUser currentUser;

    /**
     * Upload de la photo AVANT de creer le signalement : le frontend appelle
     * cette route en premier, recupere "photoUrl" dans la reponse, puis
     * l'inclut dans le POST /signalements qui suit. Compression automatique
     * a 800px max cote serveur (cf. PhotoStorageService), rien n'est stocke
     * en base -- seulement sur disque, l'URL renvoyee ici est ce qui va dans
     * Signalement.photoUrl.
     */
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploaderPhoto(@RequestParam("fichier") MultipartFile fichier) {
        String photoUrl = photoStorageService.stocker(fichier);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("photoUrl", photoUrl));
    }

    /** BF-SIG-01 a 04 (role CITOYEN). */
    @PostMapping
    public ResponseEntity<SignalementResponseDTO> creer(@Valid @RequestBody SignalementCreateDTO requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(signalementService.creer(currentUser.id(), requete));
    }

    /** BF-SIG-05 : historique du citoyen connecte. */
    @GetMapping("/mes-signalements")
    public List<SignalementResponseDTO> mesSignalements() {
        return signalementService.mesSignalements(currentUser.id());
    }

    /** Fil "marketplace" de l'entreprise connectee : signalements de son type, triés par distance. */
    @GetMapping("/marketplace")
    public List<SignalementMarketplaceDTO> fluxEntreprise() {
        return signalementService.fluxEntreprise(currentUser.id());
    }

    /** L'entreprise connectee prend en charge le signalement (premier arrive, premier servi). */
    @PatchMapping("/{id}/prendre-en-charge")
    public SignalementResponseDTO prendreEnCharge(@PathVariable Long id) {
        return signalementService.prendreEnCharge(id, currentUser.id());
    }

    /** L'entreprise confirme la collecte physique (uniquement si c'est elle qui l'a pris). */
    @PatchMapping("/{id}/collecter")
    public SignalementResponseDTO marquerCollecte(@PathVariable Long id) {
        return signalementService.marquerCollecte(id, currentUser.id());
    }

    /** BF-BO-02 : liste complete pour la carte interactive (role ADMIN). */
    @GetMapping("/admin")
    public List<SignalementResponseDTO> tousLesSignalements() {
        return signalementService.tousLesSignalements();
    }

    /** BF-BO-02 : mise a jour du statut (role ADMIN). */
    @PatchMapping("/admin/{id}/statut")
    public SignalementResponseDTO changerStatut(@PathVariable Long id, @Valid @RequestBody StatutUpdateDTO requete) {
        return signalementService.changerStatut(id, requete.getStatut());
    }

    /** BF-BO-03 : export CSV des signalements (role ADMIN). */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exporter() {
        List<SignalementResponseDTO> signalements = signalementService.tousLesSignalements();
        String header = "id;numeroSuivi;typeDechet;statut;latitude;longitude;dateCreation\n";
        String lignes = signalements.stream()
                .map(s -> String.join(";",
                        String.valueOf(s.getId()),
                        s.getNumeroSuivi(),
                        s.getTypeDechet().name(),
                        s.getStatut().name(),
                        String.valueOf(s.getLatitude()),
                        String.valueOf(s.getLongitude()),
                        String.valueOf(s.getDateCreation())))
                .collect(Collectors.joining("\n"));

        byte[] csv = (header + lignes).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=signalements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
