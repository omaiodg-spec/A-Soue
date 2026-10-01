import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ProduitService } from '../../../core/services/produit.service';
import { PanierService } from '../../../core/services/panier.service';
import { AuthService } from '../../../core/services/auth.service';
import { Produit } from '../../../core/models/produit.model';

@Component({
  selector: 'app-detail-produit',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './detail-produit.html',
})
export class DetailProduit implements OnInit {
  private readonly produitService = inject(ProduitService);
  readonly panierService = inject(PanierService);
  readonly authService = inject(AuthService);

  readonly id = input.required<string>();
  produit = signal<Produit | null>(null);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  ajoute = signal(false);

  ngOnInit(): void {
    this.produitService.obtenir(Number(this.id())).subscribe({
      next: (data) => {
        this.produit.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Ce produit est introuvable.');
        this.chargement.set(false);
      },
    });
  }

  ajouterAuPanier(): void {
    const produit = this.produit();
    if (!produit) return;
    this.panierService.ajouter(produit);
    this.ajoute.set(true);
    setTimeout(() => this.ajoute.set(false), 2000);
  }
}
