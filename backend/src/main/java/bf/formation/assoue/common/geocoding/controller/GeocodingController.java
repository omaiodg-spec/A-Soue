package bf.formation.assoue.common.geocoding.controller;

import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.common.geocoding.dto.CoordonneesDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Proxy backend vers Nominatim (OpenStreetMap) : le frontend n'appelle jamais
 * Nominatim directement, il passe par ces deux routes. Interet : un seul point
 * de configuration du User-Agent et de la limite de debit (cf. GeocodingService),
 * plutot que de dupliquer cette logique cote frontend ou de laisser le frontend
 * marteler l'API publique sans controle.
 */
@RestController
@RequestMapping("/geocodage")
@RequiredArgsConstructor
@Tag(name = "Geocodage")
public class GeocodingController {

    private final GeocodingService geocodingService;

    /** Coordonnees GPS -> adresse lisible (ex. pour confirmer une position avant de signaler). */
    @GetMapping("/adresse")
    public Map<String, String> obtenirAdresse(@RequestParam double latitude, @RequestParam double longitude) {
        return Map.of("adresse", geocodingService.reverse(latitude, longitude).orElse(""));
    }

    /** Adresse tapee -> coordonnees GPS (ex. pour la creation d'une entreprise sans GPS a disposition). */
    @GetMapping("/coordonnees")
    public CoordonneesDTO rechercherCoordonnees(@RequestParam String adresse) {
        return geocodingService.forward(adresse);
    }
}
