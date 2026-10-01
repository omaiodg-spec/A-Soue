package bf.formation.assoue.common.event;

/**
 * Publie par PaiementService une fois une transaction confirmee/echouee.
 *
 * CommandeService a besoin de PaiementService pour initier un paiement
 * (appel direct, synchrone). Si PaiementService appelait CommandeService en
 * retour de la meme facon, on aurait un cycle de dependances Spring
 * (CommandeService -> PaiementService -> CommandeService). Publier un
 * evenement plutot qu'appeler CommandeService directement casse ce cycle.
 */
public record PaiementConfirmeEvent(Long commandeId, boolean succes) {
}
