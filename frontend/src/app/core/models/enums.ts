// Enums miroir des enums backend (bf.formation.assoue.*.model)

export type Role = 'CITOYEN' | 'ENTREPRISE' | 'ADMIN';

export type TypeDechet = 'PNEU' | 'PLASTIQUE' | 'AUTRE';
export const LIBELLES_TYPE_DECHET: Record<TypeDechet, string> = {
  PNEU: 'Pneu',
  PLASTIQUE: 'Plastique',
  AUTRE: 'Autre',
};

export type StatutSignalement = 'EN_ATTENTE' | 'PRIS_EN_CHARGE' | 'COLLECTE';
export const LIBELLES_STATUT_SIGNALEMENT: Record<StatutSignalement, string> = {
  EN_ATTENTE: 'En attente',
  PRIS_EN_CHARGE: 'Pris en charge',
  COLLECTE: 'Collecté',
};

export type StatutCommande =
  | 'EN_ATTENTE_PAIEMENT'
  | 'CONFIRMEE'
  | 'EN_PREPARATION'
  | 'LIVREE'
  | 'ECHOUEE';
export const LIBELLES_STATUT_COMMANDE: Record<StatutCommande, string> = {
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  CONFIRMEE: 'Confirmée',
  EN_PREPARATION: 'En préparation',
  LIVREE: 'Livrée',
  ECHOUEE: 'Échouée',
};

export type Operateur = 'ORANGE_MONEY' | 'MOOV_MONEY';
export const LIBELLES_OPERATEUR: Record<Operateur, string> = {
  ORANGE_MONEY: 'Orange Money',
  MOOV_MONEY: 'Moov Money',
};

export type StatutTransaction = 'INITIEE' | 'CONFIRMEE' | 'ECHOUEE';
