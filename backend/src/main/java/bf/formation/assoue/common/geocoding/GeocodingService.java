package bf.formation.assoue.common.geocoding;

import bf.formation.assoue.common.geocoding.dto.CoordonneesDTO;
import bf.formation.assoue.common.geocoding.exception.GeocodageIndisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Optional;

/**
 * Integration avec l'API Nominatim d'OpenStreetMap (https://nominatim.org) :
 * - reverse() : coordonnees GPS -> adresse lisible (utilise pour les signalements)
 * - forward() : adresse tapee -> coordonnees GPS (utilise pour la creation d'entreprise)
 *
 * IMPORTANT -- politique d'usage de l'instance publique nominatim.openstreetmap.org :
 * - maximum 1 requete par seconde, imposee cote client (pas de cle API, service
 *   gratuit et mutualise) -- cf. rateLimiter() ci-dessous ;
 * - un en-tete User-Agent identifiant l'application est OBLIGATOIRE (geocoding.user-agent) ;
 * - pas d'usage intensif : pour un volume important (plusieurs requetes/seconde en
 *   continu), passer a un fournisseur paye compatible Nominatim (LocationIQ,
 *   Geoapify, Mapbox) plutot que d'auto-heberger Nominatim (tres lourd : necessite
 *   l'import complet des donnees OpenStreetMap, plusieurs dizaines de Go).
 *
 * Chaque appel est deliberement "best effort" cote appelant (SignalementService,
 * EntrepriseService) : une panne ou une lenteur de Nominatim ne doit jamais
 * empecher de creer un signalement ou une entreprise.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private static final Object VERROU_DEBIT = new Object();
    private static volatile long dernierAppelEpochMs = 0;
    private static final long DELAI_MIN_ENTRE_APPELS_MS = 1100; // > 1s, marge de securite

    private final RestTemplate restTemplate;

    @Value("${geocoding.nominatim-url}")
    private String nominatimUrl;

    @Value("${geocoding.user-agent}")
    private String userAgent;

    /** Coordonnees GPS -> adresse lisible. Vide si Nominatim ne trouve rien ou est indisponible. */
    public Optional<String> reverse(double latitude, double longitude) {
        try {
            respecterLimiteDebit();

            String url = UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/reverse")
                    .queryParam("format", "json")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("zoom", 18)
                    .queryParam("addressdetails", 0)
                    .toUriString();

            JsonNode reponse = appeler(url);
            if (reponse == null || reponse.path("display_name").isMissingNode()) {
                return Optional.empty();
            }
            return Optional.of(reponse.get("display_name").asText());
        } catch (Exception e) {
            log.warn("Reverse geocoding Nominatim echoue pour ({}, {})", latitude, longitude, e);
            return Optional.empty();
        }
    }

    /**
     * Adresse tapee -> coordonnees GPS.
     * @throws GeocodageIndisponibleException si aucun resultat n'est trouve -- contrairement a
     *         reverse(), l'appelant (creation d'entreprise) a besoin d'un resultat pour continuer,
     *         donc l'echec doit remonter clairement plutot que d'etre avale silencieusement.
     */
    public CoordonneesDTO forward(String adresse) {
        try {
            respecterLimiteDebit();

            String url = UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/search")
                    .queryParam("format", "json")
                    .queryParam("q", adresse)
                    .queryParam("limit", 1)
                    .toUriString();

            JsonNode reponse = appeler(url);
            if (reponse == null || !reponse.isArray() || reponse.isEmpty()) {
                throw new GeocodageIndisponibleException("Aucune coordonnee trouvee pour l'adresse : " + adresse);
            }

            JsonNode premier = reponse.get(0);
            return CoordonneesDTO.builder()
                    .latitude(Double.parseDouble(premier.get("lat").asText()))
                    .longitude(Double.parseDouble(premier.get("lon").asText()))
                    .adresseTrouvee(premier.path("display_name").asText(adresse))
                    .build();
        } catch (GeocodageIndisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Forward geocoding Nominatim echoue pour \"{}\"", adresse, e);
            throw new GeocodageIndisponibleException(
                    "Service de geocodage indisponible pour le moment, reessayez plus tard");
        }
    }

    private JsonNode appeler(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, Locale.FRENCH.getLanguage());

        ResponseEntity<JsonNode> reponse = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return reponse.getBody();
    }

    /**
     * Impose au moins 1.1s entre deux appels sortants vers Nominatim, tous threads confondus
     * (cf. politique d'usage de l'instance publique). Bloque le thread appelant si necessaire :
     * acceptable ici car chaque appel est deja "best effort" avec un timeout court (RestClientConfig).
     * NB : ce verrou est local a une instance de l'application -- s'il y a plusieurs instances
     * derriere un load balancer, la limite globale n'est plus garantie ; passer alors a un
     * fournisseur paye (pas de limite a 1 req/s) ou a un limiteur partage (ex. Redis).
     */
    private void respecterLimiteDebit() throws InterruptedException {
        synchronized (VERROU_DEBIT) {
            long attente = DELAI_MIN_ENTRE_APPELS_MS - (System.currentTimeMillis() - dernierAppelEpochMs);
            if (attente > 0) {
                Thread.sleep(attente);
            }
            dernierAppelEpochMs = System.currentTimeMillis();
        }
    }
}
