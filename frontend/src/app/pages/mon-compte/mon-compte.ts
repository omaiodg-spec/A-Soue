import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { Utilisateur } from '../../core/models/utilisateur.model';

@Component({
  selector: 'app-mon-compte',
  imports: [ReactiveFormsModule],
  templateUrl: './mon-compte.html',
})
export class MonCompte implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  utilisateur = signal<Utilisateur | null>(null);
  chargement = signal(true);
  enregistrementEnCours = signal(false);
  erreur = signal<string | null>(null);
  messageSucces = signal<string | null>(null);

  form = this.fb.group({
    motDePasseActuel: ['', Validators.required],
    nom: [''],
    telephone: [''],
    nouveauMotDePasse: ['', [Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.authService.monCompte().subscribe({
      next: (u) => {
        this.utilisateur.set(u);
        this.form.patchValue({ nom: u.nom, telephone: u.telephone });
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger votre compte.');
        this.chargement.set(false);
      },
    });
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.messageSucces.set(null);
    this.enregistrementEnCours.set(true);

    const valeurs = this.form.getRawValue();
    this.authService.mettreAJourMonCompte({
      motDePasseActuel: valeurs.motDePasseActuel!,
      nom: valeurs.nom || undefined,
      telephone: valeurs.telephone || undefined,
      nouveauMotDePasse: valeurs.nouveauMotDePasse || undefined,
    }).subscribe({
      next: (u) => {
        this.utilisateur.set(u);
        this.enregistrementEnCours.set(false);
        this.messageSucces.set('Compte mis à jour avec succès.');
        this.form.patchValue({ motDePasseActuel: '', nouveauMotDePasse: '' });
      },
      error: (err) => {
        this.enregistrementEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Mot de passe actuel incorrect ou données invalides.');
      },
    });
  }
}
