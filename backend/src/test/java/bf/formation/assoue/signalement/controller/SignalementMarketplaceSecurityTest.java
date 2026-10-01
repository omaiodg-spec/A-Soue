package bf.formation.assoue.signalement.controller;

import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.service.EntrepriseService;
import bf.formation.assoue.signalement.model.Signalement;
import bf.formation.assoue.signalement.model.StatutSignalement;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.signalement.repository.SignalementRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie que le fil marketplace et les actions de prise en charge / collecte
 * sont strictement reserves aux comptes ENTREPRISE, et que le back-office
 * (routes /admin/**) reste reserve a l'ADMIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignalementMarketplaceSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SignalementRepository signalementRepository;

    @MockBean private EntrepriseService entrepriseService;

    private Signalement signalementOuvert;

    @BeforeEach
    void setUp() {
        signalementRepository.deleteAll();
        signalementOuvert = signalementRepository.save(Signalement.builder()
                .utilisateurId(1L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("p.jpg")
                .latitude(12.37).longitude(-1.52).numeroSuivi("SIG-TEST")
                .statut(StatutSignalement.EN_ATTENTE).build());
    }

    @Test
    void marketplace_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        mockMvc.perform(get("/signalements/marketplace")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void marketplace_shouldReturn401or403_sansToken() throws Exception {
        mockMvc.perform(get("/signalements/marketplace"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void marketplace_shouldReturn200_whenTokenIsEntreprise() throws Exception {
        when(entrepriseService.obtenir(99L)).thenReturn(EntrepriseResponseDTO.builder()
                .id(99L).raisonSociale("Recyclage Faso").telephone("70000001")
                .latitude(12.37).longitude(-1.52).typesDechetGeres(Set.of(TypeDechet.PLASTIQUE)).build());

        mockMvc.perform(get("/signalements/marketplace")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(99L, "ENTREPRISE")))
                .andExpect(status().isOk());
    }

    @Test
    void prendreEnCharge_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        mockMvc.perform(patch("/signalements/" + signalementOuvert.getId() + "/prendre-en-charge")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void prendreEnCharge_shouldReturn200_whenTokenIsEntreprise() throws Exception {
        mockMvc.perform(patch("/signalements/" + signalementOuvert.getId() + "/prendre-en-charge")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(99L, "ENTREPRISE")))
                .andExpect(status().isOk());
    }

    @Test
    void prendreEnCharge_shouldReturn409_whenDejaPris() throws Exception {
        mockMvc.perform(patch("/signalements/" + signalementOuvert.getId() + "/prendre-en-charge")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(99L, "ENTREPRISE")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/signalements/" + signalementOuvert.getId() + "/prendre-en-charge")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(42L, "ENTREPRISE")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminRoutes_shouldReturn403_whenTokenIsEntreprise() throws Exception {
        mockMvc.perform(get("/signalements/admin")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(99L, "ENTREPRISE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoutes_shouldReturn200_whenTokenIsAdmin() throws Exception {
        mockMvc.perform(get("/signalements/admin")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ADMIN")))
                .andExpect(status().isOk());
    }
}
