package bf.formation.assoue.signalement.controller;

import bf.formation.assoue.auth.service.EntrepriseService;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.signalement.dto.SignalementCreateDTO;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.signalement.repository.SignalementRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignalementControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SignalementRepository signalementRepository;

    @MockBean private EntrepriseService entrepriseService;
    // Evite un vrai appel reseau vers Nominatim pendant les tests (cf. GeocodingService).
    @MockBean private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        signalementRepository.deleteAll();
        when(entrepriseService.listerParType(any())).thenReturn(Collections.emptyList());
    }

    private SignalementCreateDTO nouveauSignalement() {
        SignalementCreateDTO dto = new SignalementCreateDTO();
        dto.setTypeDechet(TypeDechet.PLASTIQUE);
        dto.setPhotoUrl("https://exemple.com/photo.jpg");
        dto.setLatitude(12.3714);
        dto.setLongitude(-1.5197);
        return dto;
    }

    @Test
    void creerSignalement_shouldReturn201_whenCitoyenAuthentifie() throws Exception {
        String token = JwtTestUtils.token(10L, "CITOYEN");

        mockMvc.perform(post("/signalements")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(nouveauSignalement())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroSuivi").isNotEmpty())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    void creerSignalement_shouldReturn401or403_sansToken() throws Exception {
        mockMvc.perform(post("/signalements")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(nouveauSignalement())))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void mesSignalements_shouldOnlyReturnOwnSignalements() throws Exception {
        String tokenCitoyen1 = JwtTestUtils.token(1L, "CITOYEN");
        String tokenCitoyen2 = JwtTestUtils.token(2L, "CITOYEN");

        mockMvc.perform(post("/signalements")
                .header("Authorization", "Bearer " + tokenCitoyen1)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(nouveauSignalement())));

        mockMvc.perform(get("/signalements/mes-signalements")
                        .header("Authorization", "Bearer " + tokenCitoyen2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/signalements/mes-signalements")
                        .header("Authorization", "Bearer " + tokenCitoyen1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
