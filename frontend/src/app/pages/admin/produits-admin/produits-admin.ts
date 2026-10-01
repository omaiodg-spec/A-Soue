import { Component, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ProduitService } from '../../../core/services/produit.service';
import { Produit } from '../../../core/models/produit.model';

@Component({
  selector: 'app-produits-admin',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './produits-admin.html',
})
export class ProduitsAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly produitService = inject(ProduitService);

  produits = signal<Produit[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  enregistrementEnCours = signal(false);
  idEnEdition = signal<number | null>(null);

  form = this.fb.group({
    titre: ['', Validators.required],
    description: [''],
    photoUrl: [''],
    prixFcfa: [0, [Validators.required, Validators.min(1)]],
    disponible: [true],
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.produitService.lister().subscribe({
      next: (data) => {
        this.produits.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger le catalogue.');
        this.chargement.set(false);
      },
    });
  }

  editer(produit: Produit): void {
    this.idEnEdition.set(produit.id);
    this.form.patchValue({
      titre: produit.titre,
      description: produit.description ?? '',
      photoUrl: produit.photoUrl ?? '',
      prixFcfa: produit.prixFcfa,
      disponible: produit.disponible,
    });
  }

  annulerEdition(): void {
    this.idEnEdition.set(null);
    this.form.reset({ disponible: true, prixFcfa: 0 });
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valeurs = this.form.getRawValue();
    const payload = {
      titre: valeurs.titre!,
      description: valeurs.description || undefined,
      photoUrl: valeurs.photoUrl || undefined,
      prixFcfa: valeurs.prixFcfa!,
      disponible: valeurs.disponible!,
    };

    this.erreur.set(null);
    this.enregistrementEnCours.set(true);
    const idActuel = this.idEnEdition();
    const requete = idActuel ? this.produitService.modifier(idActuel, payload) : this.produitService.creer(payload);

    requete.subscribe({
      next: () => {
        this.enregistrementEnCours.set(false);
        this.annulerEdition();
        this.charger();
      },
      error: () => {
        this.enregistrementEnCours.set(false);
        this.erreur.set('Impossible d\'enregistrer ce produit.');
      },
    });
  }

  supprimer(produit: Produit): void {
    if (!confirm(`Supprimer "${produit.titre}" ?`)) return;
    this.produitService.supprimer(produit.id).subscribe({
      next: () => this.charger(),
      error: () => this.erreur.set('Impossible de supprimer ce produit.'),
    });
  }
}
