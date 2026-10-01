package bf.formation.assoue.common.geocoding;

import bf.formation.assoue.common.geocoding.dto.CoordonneesDTO;
import bf.formation.assoue.common.geocoding.exception.GeocodageIndisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeocodingService geocodingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        geocodingService = new GeocodingService(restTemplate);
        ReflectionTestUtils.setField(geocodingService, "nominatimUrl", "https://nominatim.openstreetmap.org");
        ReflectionTestUtils.setField(geocodingService, "userAgent", "AssoueStoreTest/1.0 (test@assoue.bf)");
    }

    private JsonNode json(String contenu) throws Exception {
        return objectMapper.readTree(contenu);
    }

    @Test
    void reverse_shouldReturnDisplayName_whenNominatimRespondsOk() throws Exception {
        JsonNode reponse = json("""
                {
                  "display_name": "Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso"
                }
                """);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(reponse));

        var result = geocodingService.reverse(12.3714, -1.5197);

        assertThat(result).contains("Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso");
    }

    @Test
    void reverse_shouldReturnEmpty_whenNominatimThrows() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("timeout"));

        var result = geocodingService.reverse(12.3714, -1.5197);

        assertThat(result).isEmpty();
    }

    @Test
    void reverse_shouldReturnEmpty_whenNoDisplayNameInResponse() throws Exception {
        JsonNode reponse = json("""
                {
                  "error": "Unable to geocode"
                }
                """);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(reponse));

        var result = geocodingService.reverse(0.0, 0.0);

        assertThat(result).isEmpty();
    }

    @Test
    void forward_shouldReturnCoordinates_whenNominatimFindsAMatch() throws Exception {
        JsonNode reponse = json("""
                [{
                  "lat": "12.3714",
                  "lon": "-1.5197",
                  "display_name": "Ouagadougou, Burkina Faso"
                }]
                """);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(reponse));

        CoordonneesDTO result = geocodingService.forward("Ouagadougou");

        assertThat(result.getLatitude()).isEqualTo(12.3714);
        assertThat(result.getLongitude()).isEqualTo(-1.5197);
        assertThat(result.getAdresseTrouvee()).isEqualTo("Ouagadougou, Burkina Faso");
    }

    @Test
    void forward_shouldThrow_whenNoResultsFound() throws Exception {
        JsonNode reponse = json("[]");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(reponse));

        assertThatThrownBy(() -> geocodingService.forward("Adresse qui n'existe pas du tout"))
                .isInstanceOf(GeocodageIndisponibleException.class);
    }

    @Test
    void forward_shouldThrow_whenNominatimUnavailable() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("connexion refusee"));

        assertThatThrownBy(() -> geocodingService.forward("Ouagadougou"))
                .isInstanceOf(GeocodageIndisponibleException.class);
    }
}
