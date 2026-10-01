package bf.formation.assoue.commande.service;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
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
import bf.formation.assoue.commande.model.LigneCommande;
import bf.formation.assoue.commande.model.StatutCommande;
import bf.formation.assoue.commande.repository.CommandeRepository;
import bf.formation.assoue.common.event.PaiementConfirmeEvent;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import bf.formation.assoue.paiement.dto.InitierPaiementDTO;
import bf.formation.assoue.paiement.dto.TransactionResponseDTO;
import bf.formation.assoue.paiement.service.PaiementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final CommandeMapper commandeMapper;
    private final ProduitService produitService;
    private final PaiementService paiementService;
    private final SmsGatewayService smsGatewayService;
    private final AuthService authService;

    /** BF-SHOP-02 + BF-SHOP-03 : valide le panier, cree la commande, demarre le paiement. */
    public CommandeResponseDTO creer(Long utilisateurId, CommandeCreateDTO requete) {
        Commande commande = Commande.builder()
                .utilisateurId(utilisateurId)
                .montantTotal(BigDecimal.ZERO)
                .statut(StatutCommande.EN_ATTENTE_PAIEMENT)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PanierItemDTO item : requete.getLignes()) {
            // Le prix et la disponibilite viennent de ProduitService : jamais du panier
            // envoye par le client, pour eviter toute manipulation du prix cote frontend.
            ProduitResponseDTO produit;
            try {
                produit = produitService.obtenir(item.getProduitId());
            } catch (Exception e) {
                throw new ProduitIndisponibleException(item.getProduitId());
            }
            if (produit == null || !produit.isDisponible()) {
                throw new ProduitIndisponibleException(item.getProduitId());
            }

            BigDecimal sousTotal = produit.getPrixFcfa().multiply(BigDecimal.valueOf(item.getQuantite()));
            total = total.add(sousTotal);

            commande.ajouterLigne(LigneCommande.builder()
                    .produitId(item.getProduitId())
                    .quantite(item.getQuantite())
                    .prixUnitaire(produit.getPrixFcfa())
                    .build());
        }
        commande.setMontantTotal(total);
        commande = commandeRepository.save(commande);

        // BF-SHOP-03 : demarre la transaction mobile money aupres de PaiementService.
        InitierPaiementDTO requetePaiement = new InitierPaiementDTO();
        requetePaiement.setCommandeId(commande.getId());
        requetePaiement.setOperateur(requete.getOperateur());
        requetePaiement.setMontant(total);
        TransactionResponseDTO transaction = paiementService.initier(requetePaiement);
        commande.setTransactionPaiementId(transaction.getId());
        commande = commandeRepository.save(commande);

        return commandeMapper.toDto(commande);
    }

    /**
     * Ecoute PaiementConfirmeEvent publie par PaiementService une fois la transaction
     * confirmee/echouee. Un evenement plutot qu'un appel direct evite un cycle de
     * dependances Spring (CommandeService -> PaiementService -> CommandeService),
     * puisque CommandeService a deja besoin de PaiementService pour initier un paiement.
     */
    @EventListener
    public void surPaiementConfirme(PaiementConfirmeEvent event) {
        notifierResultatPaiement(event.commandeId(), event.succes());
    }

    private void notifierResultatPaiement(Long commandeId, boolean succes) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new CommandeNotFoundException(commandeId));

        commande.setStatut(succes ? StatutCommande.CONFIRMEE : StatutCommande.ECHOUEE);
        commandeRepository.save(commande);

        if (succes) {
            // BF-SHOP-04 : recapitulatif envoye par SMS.
            try {
                UtilisateurResponseDTO utilisateur = authService.obtenirParId(commande.getUtilisateurId());
                SmsRequestDTO sms = new SmsRequestDTO();
                sms.setTelephone(utilisateur.getTelephone());
                sms.setType("CONFIRMATION_COMMANDE");
                sms.setContenu("Votre commande #" + commande.getId() + " est confirmee. Montant : "
                        + commande.getMontantTotal() + " FCFA. Merci pour votre achat responsable !");
                smsGatewayService.envoyer(sms);
            } catch (Exception e) {
                log.warn("Impossible d'envoyer le SMS recapitulatif pour la commande {}", commandeId, e);
            }
        }
    }

    /** BF-SHOP-04 : suivi des commandes du citoyen connecte. */
    public List<CommandeResponseDTO> mesCommandes(Long utilisateurId) {
        return commandeRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId).stream()
                .map(commandeMapper::toDto)
                .toList();
    }

    /** BF-BO : suivi de toutes les commandes (role ADMIN). */
    public List<CommandeResponseDTO> toutes() {
        return commandeRepository.findAll().stream().map(commandeMapper::toDto).toList();
    }

    /** BF-SHOP-05 : mise a jour du statut logistique (En preparation / Livree) par l'ADMIN. */
    public CommandeResponseDTO changerStatut(Long id, StatutCommande statut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new CommandeNotFoundException(id));
        commande.setStatut(statut);
        return commandeMapper.toDto(commandeRepository.save(commande));
    }
}
