package bf.formation.assoue.commande.service;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.service.AuthService;
import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.service.ProduitService;
import bf.formation.assoue.commande.dto.CommandeCreateDTO;
import bf.formation.assoue.commande.dto.CommandeResponseDTO;
import bf.formation.assoue.commande.dto.PanierItemDTO;
import bf.formation.assoue.commande.exception.CommandeNotFoundException;
import bf.formation.assoue.commande.exception.ProduitIndisponibleException;
import bf.formation.assoue.commande.mapper.CommandeMapper;
import bf.formation.assoue.commande.model.Commande;
import bf.formation.assoue.common.event.PaiementConfirmeEvent;
import bf.formation.assoue.common.model.Operateur;
import bf.formation.assoue.commande.model.StatutCommande;
import bf.formation.assoue.commande.repository.CommandeRepository;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.model.StatutTransaction;
import bf.formation.assoue.paiement.service.PaiementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Auparavant ProduitClient/PaiementClient/NotificationClient/UtilisateurClient
 * (Feign) etaient mockes. Avec la fusion, CommandeService injecte directement
 * ProduitService/PaiementService/SmsGatewayService/AuthService : ce sont eux
 * qu'on mocke ici pour garder l'isolation du test unitaire.
 *
 * "notifierResultatPaiement" est desormais prive et declenche via
 * PaiementConfirmeEvent (cf. surPaiementConfirme) plutot qu'appele
 * directement -- les tests appellent donc surPaiementConfirme.
 */
@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock private CommandeRepository commandeRepository;
    @Mock private ProduitService produitService;
    @Mock private PaiementService paiementService;
    @Mock private SmsGatewayService smsGatewayService;
    @Mock private AuthService authService;
    private final CommandeMapper commandeMapper = new CommandeMapper();

    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        commandeService = new CommandeService(commandeRepository, commandeMapper, produitService,
                paiementService, smsGatewayService, authService);
    }

    @Test
    void creer_shouldComputeTotalFromCatalogueAndInitierPaiement() {
        PanierItemDTO item = new PanierItemDTO();
        item.setProduitId(5L);
        item.setQuantite(2);
        CommandeCreateDTO requete = new CommandeCreateDTO();
        requete.setLignes(List.of(item));
        requete.setOperateur(Operateur.ORANGE_MONEY);

        when(produitService.obtenir(5L)).thenReturn(
                ProduitResponseDTO.builder().id(5L).titre("Pagne recycle").prixFcfa(BigDecimal.valueOf(3000)).disponible(true).build());
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> {
            Commande c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });
        when(paiementService.initier(any())).thenReturn(
                TransactionResponseDTO.builder().id(100L).commandeId(1L).operateur(Operateur.ORANGE_MONEY)
                        .montant(BigDecimal.valueOf(6000)).statut(StatutTransaction.INITIEE).referenceExterne("SIM-XYZ").build());

        CommandeResponseDTO result = commandeService.creer(10L, requete);

        assertThat(result.getMontantTotal()).isEqualByComparingTo("6000");
        assertThat(result.getStatut()).isEqualTo(StatutCommande.EN_ATTENTE_PAIEMENT);
        assertThat(result.getLignes()).hasSize(1);
        verify(paiementService).initier(argThat(req -> req.getMontant().compareTo(BigDecimal.valueOf(6000)) == 0));
    }

    @Test
    void creer_shouldThrow_whenProduitIndisponible() {
        PanierItemDTO item = new PanierItemDTO();
        item.setProduitId(5L);
        item.setQuantite(1);
        CommandeCreateDTO requete = new CommandeCreateDTO();
        requete.setLignes(List.of(item));
        requete.setOperateur(Operateur.MOOV_MONEY);

        when(produitService.obtenir(5L)).thenReturn(
                ProduitResponseDTO.builder().id(5L).titre("Pagne recycle").prixFcfa(BigDecimal.valueOf(3000)).disponible(false).build());

        assertThatThrownBy(() -> commandeService.creer(10L, requete))
                .isInstanceOf(ProduitIndisponibleException.class);
        verifyNoInteractions(paiementService);
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void creer_shouldThrow_whenProduitServiceFails() {
        PanierItemDTO item = new PanierItemDTO();
        item.setProduitId(5L);
        item.setQuantite(1);
        CommandeCreateDTO requete = new CommandeCreateDTO();
        requete.setLignes(List.of(item));
        requete.setOperateur(Operateur.MOOV_MONEY);

        when(produitService.obtenir(5L)).thenThrow(new RuntimeException("produit introuvable"));

        assertThatThrownBy(() -> commandeService.creer(10L, requete))
                .isInstanceOf(ProduitIndisponibleException.class);
    }

    @Test
    void surPaiementConfirme_shouldConfirmAndSendSms_whenSucces() {
        Commande commande = Commande.builder()
                .id(1L).utilisateurId(10L).montantTotal(BigDecimal.valueOf(6000))
                .statut(StatutCommande.EN_ATTENTE_PAIEMENT).build();
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authService.obtenirParId(10L)).thenReturn(
                UtilisateurResponseDTO.builder().id(10L).nom("Awa").telephone("70000000")
                        .role(Role.CITOYEN).telephoneVerifie(true).build());

        commandeService.surPaiementConfirme(new PaiementConfirmeEvent(1L, true));

        assertThat(commande.getStatut()).isEqualTo(StatutCommande.CONFIRMEE);
        verify(smsGatewayService).envoyer(argThat((SmsRequestDTO sms) -> sms.getTelephone().equals("70000000")));
    }

    @Test
    void surPaiementConfirme_shouldSetEchouee_sansEnvoyerSms_whenEchec() {
        Commande commande = Commande.builder()
                .id(1L).utilisateurId(10L).montantTotal(BigDecimal.valueOf(6000))
                .statut(StatutCommande.EN_ATTENTE_PAIEMENT).build();
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));

        commandeService.surPaiementConfirme(new PaiementConfirmeEvent(1L, false));

        assertThat(commande.getStatut()).isEqualTo(StatutCommande.ECHOUEE);
        verifyNoInteractions(smsGatewayService, authService);
    }

    @Test
    void surPaiementConfirme_shouldThrow_whenCommandeNotFound() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandeService.surPaiementConfirme(new PaiementConfirmeEvent(99L, true)))
                .isInstanceOf(CommandeNotFoundException.class);
    }

    @Test
    void surPaiementConfirme_shouldNotFail_whenSmsSendingThrows() {
        Commande commande = Commande.builder()
                .id(1L).utilisateurId(10L).montantTotal(BigDecimal.valueOf(6000))
                .statut(StatutCommande.EN_ATTENTE_PAIEMENT).build();
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authService.obtenirParId(10L)).thenThrow(new RuntimeException("utilisateur introuvable"));

        commandeService.surPaiementConfirme(new PaiementConfirmeEvent(1L, true));

        assertThat(commande.getStatut()).isEqualTo(StatutCommande.CONFIRMEE);
    }
}
