import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { FormationService } from '../../../core/services/formation.service';
import { Formation } from '../../../core/models/formation.model';
import { Utilisateur } from '../../../core/models/utilisateur.model';

@Component({
  selector: 'app-formations-admin',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './formations-admin.html',
})
export class FormationsAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly formationService = inject(FormationService);

  formations = signal<Formation[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  creationEnCours = signal(false);

  formationOuverteId = signal<number | null>(null);
  inscrits = signal<Utilisateur[]>([]);
  chargementInscrits = signal(false);

  form = this.fb.group({
    titre: ['', Validators.required],
    description: [''],
    lieu: ['', Validators.required],
    dateFormation: ['', Validators.required],
    placesDisponibles: [null as number | null],
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.formationService.lister().subscribe({
      next: (data) => {
        this.formations.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les formations.');
        this.chargement.set(false);
      },
    });
  }

  creer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.erreur.set(null);
    this.creationEnCours.set(true);
    this.formationService.creer({
      titre: v.titre!,
      description: v.description || undefined,
      lieu: v.lieu!,
      dateFormation: new Date(v.dateFormation!).toISOString(),
      placesDisponibles: v.placesDisponibles || null,
    }).subscribe({
      next: () => {
        this.creationEnCours.set(false);
        this.form.reset();
        this.charger();
      },
      error: (err) => {
        this.creationEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Impossible de créer cette formation (la date doit être dans le futur).');
      },
    });
  }

  voirInscrits(formation: Formation): void {
    if (this.formationOuverteId() === formation.id) {
      this.formationOuverteId.set(null);
      return;
    }
    this.formationOuverteId.set(formation.id);
    this.chargementInscrits.set(true);
    this.formationService.listerInscrits(formation.id).subscribe({
      next: (data) => {
        this.inscrits.set(data);
        this.chargementInscrits.set(false);
      },
      error: () => {
        this.chargementInscrits.set(false);
      },
    });
  }
}
