import { computed, Injectable, signal } from '@angular/core';
import { Produit } from '../models/produit.model';

export interface LignePanier {
  produit: Produit;
  quantite: number;
}

// Panier tenu en mémoire côté client (le backend ne connaît que les lignes
// envoyées à POST /commandes, il ne gère pas de panier persistant).
@Injectable({ providedIn: 'root' })
export class PanierService {
  private readonly lignesInternes = signal<LignePanier[]>([]);
  readonly lignes = this.lignesInternes.asReadonly();

  readonly total = computed(() =>
    this.lignesInternes().reduce((somme, ligne) => somme + ligne.produit.prixFcfa * ligne.quantite, 0)
  );

  readonly nombreArticles = computed(() =>
    this.lignesInternes().reduce((somme, ligne) => somme + ligne.quantite, 0)
  );

  ajouter(produit: Produit): void {
    const lignes = this.lignesInternes();
    const existante = lignes.find((l) => l.produit.id === produit.id);
    if (existante) {
      this.modifierQuantite(produit.id, existante.quantite + 1);
    } else {
      this.lignesInternes.set([...lignes, { produit, quantite: 1 }]);
    }
  }

  modifierQuantite(produitId: number, quantite: number): void {
    if (quantite <= 0) {
      this.retirer(produitId);
      return;
    }
    this.lignesInternes.set(
      this.lignesInternes().map((l) => (l.produit.id === produitId ? { ...l, quantite } : l))
    );
  }

  retirer(produitId: number): void {
    this.lignesInternes.set(this.lignesInternes().filter((l) => l.produit.id !== produitId));
  }

  vider(): void {
    this.lignesInternes.set([]);
  }
}
