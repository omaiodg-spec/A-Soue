import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalementService } from '../../../core/services/signalement.service';
import { AuthService } from '../../../core/services/auth.service';
import { BadgeStatut } from '../../../shared/badge-statut/badge-statut';
import { Signalement } from '../../../core/models/signalement.model';
import { LIBELLES_STATUT_SIGNALEMENT, LIBELLES_TYPE_DECHET, StatutSignalement } from '../../../core/models/enums';

@Component({
  selector: 'app-signalements-admin',
  imports: [DatePipe, FormsModule, BadgeStatut],
  templateUrl: './signalements-admin.html',
})
export class SignalementsAdmin implements OnInit {
  private readonly signalementService = inject(SignalementService);
  private readonly authService = inject(AuthService);

  signalements = signal<Signalement[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  readonly libellesStatut = LIBELLES_STATUT_SIGNALEMENT;
  readonly libellesType = LIBELLES_TYPE_DECHET;
  readonly statuts: StatutSignalement[] = ['EN_ATTENTE', 'PRIS_EN_CHARGE', 'COLLECTE'];

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.signalementService.tousLesSignalements().subscribe({
      next: (data) => {
        this.signalements.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les signalements.');
        this.chargement.set(false);
      },
    });
  }

  changerStatut(s: Signalement, statut: StatutSignalement): void {
    this.signalementService.changerStatut(s.id, statut).subscribe({
      next: () => this.charger(),
      error: () => this.erreur.set('Impossible de changer le statut de ce signalement.'),
    });
  }

  variante(statut: StatutSignalement): 'succes' | 'attente' | 'info' {
    if (statut === 'COLLECTE') return 'succes';
    if (statut === 'EN_ATTENTE') return 'attente';
    return 'info';
  }

  // L'export CSV exige le token JWT (route protégée ADMIN) : on ne peut pas
  // simplement faire pointer un <a href> dessus, on télécharge via fetch.
  exporterCsv(): void {
    fetch(this.signalementService.urlExport(), {
      headers: { Authorization: `Bearer ${this.authService.token}` },
    })
      .then((r) => r.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'signalements.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      })
      .catch(() => this.erreur.set("Échec de l'export CSV."));
  }
}
