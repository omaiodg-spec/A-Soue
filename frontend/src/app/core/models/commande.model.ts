import { Operateur, StatutCommande } from './enums';

export interface LigneCommande {
  produitId: number;
  quantite: number;
  prixUnitaire: number;
}

export interface Commande {
  id: number;
  utilisateurId: number;
  montantTotal: number;
  statut: StatutCommande;
  lignes: LigneCommande[];
  dateCreation: string;
}

export interface PanierItem {
  produitId: number;
  quantite: number;
}

export interface CommandeCreate {
  lignes: PanierItem[];
  operateur: Operateur;
}
