import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { SignalementService } from '../../../core/services/signalement.service';
import { Signalement } from '../../../core/models/signalement.model';
import { LIBELLES_STATUT_SIGNALEMENT, LIBELLES_TYPE_DECHET, StatutSignalement } from '../../../core/models/enums';

@Component({
  selector: 'app-mes-signalements',
  imports: [DatePipe],
  templateUrl: './mes-signalements.html',
})
export class MesSignalements implements OnInit {
  private readonly signalementService = inject(SignalementService);

  signalements = signal<Signalement[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  readonly libellesStatut = LIBELLES_STATUT_SIGNALEMENT;
  readonly libellesType = LIBELLES_TYPE_DECHET;

  ngOnInit(): void {
    this.signalementService.mesSignalements().subscribe({
      next: (data) => {
        this.signalements.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger vos signalements.');
        this.chargement.set(false);
      },
    });
  }

  urlPhoto(url: string): string {
    return this.signalementService.urlPhoto(url);
  }

  variante(statut: StatutSignalement): 'succes' | 'attente' | 'info' {
    if (statut === 'COLLECTE') return 'succes';
    if (statut === 'EN_ATTENTE') return 'attente';
    return 'info';
  }
}
