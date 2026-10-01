import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormationService } from '../../../core/services/formation.service';
import { Formation } from '../../../core/models/formation.model';

@Component({
  selector: 'app-liste-formations',
  imports: [RouterLink, DatePipe],
  templateUrl: './liste-formations.html',
})
export class ListeFormations implements OnInit {
  private readonly formationService = inject(FormationService);

  formations = signal<Formation[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.formationService.lister().subscribe({
      next: (data) => {
        this.formations.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les formations pour le moment.');
        this.chargement.set(false);
      },
    });
  }
}
