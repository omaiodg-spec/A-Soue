import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ProduitService } from '../../../core/services/produit.service';
import { PanierService } from '../../../core/services/panier.service';
import { AuthService } from '../../../core/services/auth.service';
import { Produit } from '../../../core/models/produit.model';

@Component({
  selector: 'app-liste-produits',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './liste-produits.html',
})
export class ListeProduits implements OnInit {
  private readonly produitService = inject(ProduitService);
  readonly panierService = inject(PanierService);
  readonly authService = inject(AuthService);

  produits = signal<Produit[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.produitService.lister().subscribe({
      next: (data) => {
        this.produits.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger le catalogue pour le moment.');
        this.chargement.set(false);
      },
    });
  }

  ajouterAuPanier(produit: Produit, evenement: Event): void {
    evenement.stopPropagation();
    evenement.preventDefault();
    this.panierService.ajouter(produit);
  }
}
