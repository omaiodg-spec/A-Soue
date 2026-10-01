package bf.formation.assoue.auth.model;

/**
 * Roles metier (CDC section 1C + evolution "marketplace").
 * CITOYEN    : signale des dechets et achete des produits (auto-inscription).
 * ENTREPRISE : partenaire de collecte (compte cree uniquement par l'ADMIN).
 * ADMIN      : personnel As'Soue Group (back-office). As'Soue elle-meme est
 *              enregistree comme une ENTREPRISE geree en interne pour les pneus.
 */
public enum Role {
    CITOYEN,
    ENTREPRISE,
    ADMIN
}
