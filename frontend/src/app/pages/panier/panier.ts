import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { PanierService } from '../../core/services/panier.service';
import { CommandeService } from '../../core/services/commande.service';
import { Operateur, LIBELLES_OPERATEUR } from '../../core/models/enums';

@Component({
  selector: 'app-panier',
  imports: [RouterLink, DecimalPipe, ReactiveFormsModule],
  templateUrl: './panier.html',
})
export class Panier {
  readonly panierService = inject(PanierService);
  private readonly commandeService = inject(CommandeService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly operateurs: Operateur[] = ['ORANGE_MONEY', 'MOOV_MONEY'];
  readonly libellesOperateur = LIBELLES_OPERATEUR;

  form = this.fb.group({
    operateur: ['' as Operateur | '', Validators.required],
  });

  commandeEnCours = signal(false);
  erreur = signal<string | null>(null);
  commandeConfirmee = signal<number | null>(null);

  passerCommande(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.commandeEnCours.set(true);

    const lignes = this.panierService.lignes().map((l) => ({ produitId: l.produit.id, quantite: l.quantite }));

    this.commandeService.creer({
      lignes,
      operateur: this.form.getRawValue().operateur as Operateur,
    }).subscribe({
      next: (commande) => {
        this.commandeEnCours.set(false);
        this.commandeConfirmee.set(commande.id);
        this.panierService.vider();
      },
      error: (err) => {
        this.commandeEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Impossible de créer la commande (produit indisponible ou stock insuffisant).');
      },
    });
  }
}
