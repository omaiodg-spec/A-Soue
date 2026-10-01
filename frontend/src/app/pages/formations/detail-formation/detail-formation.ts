import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormationService } from '../../../core/services/formation.service';
import { AuthService } from '../../../core/services/auth.service';
import { Formation } from '../../../core/models/formation.model';

@Component({
  selector: 'app-detail-formation',
  imports: [RouterLink, DatePipe],
  templateUrl: './detail-formation.html',
})
export class DetailFormation implements OnInit {
  private readonly formationService = inject(FormationService);
  readonly authService = inject(AuthService);

  readonly id = input.required<string>();
  formation = signal<Formation | null>(null);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  inscriptionReussie = signal(false);
  inscriptionEnCours = signal(false);

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.formationService.obtenir(Number(this.id())).subscribe({
      next: (data) => {
        this.formation.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Cette formation est introuvable.');
        this.chargement.set(false);
      },
    });
  }

  sInscrire(): void {
    this.inscriptionEnCours.set(true);
    this.erreur.set(null);
    this.formationService.sInscrire(Number(this.id())).subscribe({
      next: () => {
        this.inscriptionEnCours.set(false);
        this.inscriptionReussie.set(true);
        this.charger();
      },
      error: (err) => {
        this.inscriptionEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Inscription impossible (déjà inscrit, ou formation complète).');
      },
    });
  }
}
