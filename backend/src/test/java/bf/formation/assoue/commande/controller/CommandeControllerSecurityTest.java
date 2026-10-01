package bf.formation.assoue.commande.controller;

import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.service.ProduitService;
import bf.formation.assoue.commande.dto.CommandeCreateDTO;
import bf.formation.assoue.commande.dto.PanierItemDTO;
import bf.formation.assoue.common.model.Operateur;
import bf.formation.assoue.commande.repository.CommandeRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.model.StatutTransaction;
import bf.formation.assoue.paiement.service.PaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auparavant, ProduitClient/PaiementClient/NotificationClient/UtilisateurClient
 * (Feign) etaient mockes en @MockBean. Avec la fusion, CommandeService appelle
 * directement les beans ProduitService/PaiementService : ce sont eux qu'on
 * mocke desormais pour isoler le test au niveau HTTP, comme avant.
 *
 * L'ancien test "callbackInterne_shouldBeAccessible_sansToken" (route HTTP
 * /commandes/internal/{id}/paiement) a disparu avec elle : la confirmation
 * de paiement passe desormais par un evenement Spring interne
 * (PaiementConfirmeEvent), plus par une route HTTP a tester ici.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommandeControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CommandeRepository commandeRepository;

    @MockBean private ProduitService produitService;
    @MockBean private PaiementService paiementService;

    @BeforeEach
    void setUp() {
        commandeRepository.deleteAll();
        when(produitService.obtenir(any())).thenReturn(
                ProduitResponseDTO.builder().id(1L).titre("Pagne").prixFcfa(BigDecimal.valueOf(3000)).disponible(true).build());
        when(paiementService.initier(any())).thenReturn(
                TransactionResponseDTO.builder().id(1L).commandeId(1L).operateur(Operateur.ORANGE_MONEY)
                        .montant(BigDecimal.valueOf(3000)).statut(StatutTransaction.INITIEE).referenceExterne("SIM-1").build());
    }

    private String payload() throws Exception {
        PanierItemDTO item = new PanierItemDTO();
        item.setProduitId(1L);
        item.setQuantite(1);
        CommandeCreateDTO dto = new CommandeCreateDTO();
        dto.setLignes(List.of(item));
        dto.setOperateur(Operateur.ORANGE_MONEY);
        return objectMapper.writeValueAsString(dto);
    }

    @Test
    void creerCommande_shouldReturn201_whenCitoyenAuthentifie() throws Exception {
        mockMvc.perform(post("/commandes")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(10L, "CITOYEN"))
                        .contentType("application/json")
                        .content(payload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_PAIEMENT"));
    }

    @Test
    void creerCommande_shouldReturn401or403_sansToken() throws Exception {
        mockMvc.perform(post("/commandes").contentType("application/json").content(payload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void routesAdmin_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        mockMvc.perform(get("/commandes/admin")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(10L, "CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void routesAdmin_shouldReturn200_whenTokenIsAdmin() throws Exception {
        mockMvc.perform(get("/commandes/admin")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ADMIN")))
                .andExpect(status().isOk());
    }
}
