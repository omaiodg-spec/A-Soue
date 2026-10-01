package bf.formation.assoue.paiement.controller;

import bf.formation.assoue.paiement.repository.TransactionRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'ancien test de /paiements/initier (accessible sans token car appele en
 * Feign par service-commande) a disparu avec la fusion : CommandeService
 * appelle desormais PaiementService.initier(...) directement (methode Java,
 * plus de route HTTP /initier du tout).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaiementControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void obtenirTransaction_shouldReturn401or403_sansToken() throws Exception {
        mockMvc.perform(get("/paiements/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void obtenirTransaction_shouldReturn404_whenNotFoundAvecTokenValide() throws Exception {
        mockMvc.perform(get("/paiements/999")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }
}
