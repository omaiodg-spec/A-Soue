package bf.formation.assoue.auth.controller;

import bf.formation.assoue.auth.dto.EntrepriseCreateDTO;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.auth.repository.ProfilEntrepriseRepository;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de securite : verifie que la creation d'un compte entreprise est
 * strictement reservee a l'ADMIN, comme demande (pas d'auto-inscription).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntrepriseControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ProfilEntrepriseRepository profilEntrepriseRepository;

    // Evite un vrai appel reseau vers Nominatim pendant les tests (cf. GeocodingService).
    @MockBean private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        profilEntrepriseRepository.deleteAll();
        utilisateurRepository.deleteAll();
    }

    private String payload() throws Exception {
        EntrepriseCreateDTO dto = new EntrepriseCreateDTO();
        dto.setRaisonSociale("Recyclage Faso SARL");
        dto.setTelephone("71000000");
        dto.setMotDePasse("motdepasse123");
        dto.setLatitude(12.35);
        dto.setLongitude(-1.53);
        dto.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));
        return objectMapper.writeValueAsString(dto);
    }

    @Test
    void creerEntreprise_shouldReturn401or403_whenNoToken() throws Exception {
        mockMvc.perform(post("/entreprises").contentType("application/json").content(payload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void creerEntreprise_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        String token = JwtTestUtils.token(1L, "CITOYEN");

        mockMvc.perform(post("/entreprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void creerEntreprise_shouldReturn403_whenTokenIsEntreprise() throws Exception {
        String token = JwtTestUtils.token(1L, "ENTREPRISE");

        mockMvc.perform(post("/entreprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void creerEntreprise_shouldReturn201_whenTokenIsAdmin() throws Exception {
        String token = JwtTestUtils.token(1L, "ADMIN");

        mockMvc.perform(post("/entreprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.raisonSociale").value("Recyclage Faso SARL"));
    }

    @Test
    void creerEntreprise_shouldReturn403_whenTokenSignedWithWrongSecret() throws Exception {
        String token = JwtTestUtils.tokenSigneAvecMauvaisSecret(1L, "ADMIN");

        mockMvc.perform(post("/entreprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void creerEntreprise_shouldReturn401or403_whenTokenExpired() throws Exception {
        String token = JwtTestUtils.expiredToken(1L, "ADMIN");

        mockMvc.perform(post("/entreprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }
}
