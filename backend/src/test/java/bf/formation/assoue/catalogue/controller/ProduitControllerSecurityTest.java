package bf.formation.assoue.catalogue.controller;

import bf.formation.assoue.catalogue.dto.ProduitCreateDTO;
import bf.formation.assoue.catalogue.model.Produit;
import bf.formation.assoue.catalogue.repository.ProduitRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProduitControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProduitRepository produitRepository;

    @BeforeEach
    void setUp() {
        produitRepository.deleteAll();
    }

    private String payload() throws Exception {
        ProduitCreateDTO dto = new ProduitCreateDTO();
        dto.setTitre("Pagne recycle");
        dto.setDescription("Tisse a partir de plastique collecte");
        dto.setPrixFcfa(BigDecimal.valueOf(5000));
        dto.setDisponible(true);
        return objectMapper.writeValueAsString(dto);
    }

    @Test
    void listerProduits_shouldBeAccessible_sansToken() throws Exception {
        produitRepository.save(Produit.builder().titre("Pagne").prixFcfa(BigDecimal.TEN).disponible(true).build());

        mockMvc.perform(get("/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void creerProduit_shouldReturn401or403_sansToken() throws Exception {
        mockMvc.perform(post("/produits").contentType("application/json").content(payload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void creerProduit_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        mockMvc.perform(post("/produits")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "CITOYEN"))
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void creerProduit_shouldReturn201_whenTokenIsAdmin() throws Exception {
        mockMvc.perform(post("/produits")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ADMIN"))
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Pagne recycle"));
    }

    @Test
    void supprimerProduit_shouldReturn403_whenTokenIsEntreprise() throws Exception {
        Produit produit = produitRepository.save(Produit.builder().titre("Pagne").prixFcfa(BigDecimal.TEN).disponible(true).build());

        mockMvc.perform(delete("/produits/" + produit.getId())
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ENTREPRISE")))
                .andExpect(status().isForbidden());
    }
}
