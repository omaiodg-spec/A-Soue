export interface Produit {
  id: number;
  titre: string;
  description: string | null;
  photoUrl: string | null;
  prixFcfa: number;
  disponible: boolean;
}

export interface ProduitCreate {
  titre: string;
  description?: string;
  photoUrl?: string;
  prixFcfa: number;
  disponible: boolean;
}
