import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommandeService } from '../../../core/services/commande.service';
import { AuthService } from '../../../core/services/auth.service';
import { BadgeStatut } from '../../../shared/badge-statut/badge-statut';
import { Commande } from '../../../core/models/commande.model';
import { LIBELLES_STATUT_COMMANDE, StatutCommande } from '../../../core/models/enums';

@Component({
  selector: 'app-commandes-admin',
  imports: [DatePipe, DecimalPipe, FormsModule, BadgeStatut],
  templateUrl: './commandes-admin.html',
})
export class CommandesAdmin implements OnInit {
  private readonly commandeService = inject(CommandeService);
  private readonly authService = inject(AuthService);

  commandes = signal<Commande[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  readonly libelles = LIBELLES_STATUT_COMMANDE;
  readonly statuts: StatutCommande[] = ['EN_ATTENTE_PAIEMENT', 'CONFIRMEE', 'EN_PREPARATION', 'LIVREE', 'ECHOUEE'];

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.commandeService.toutes().subscribe({
      next: (data) => {
        this.commandes.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les commandes.');
        this.chargement.set(false);
      },
    });
  }

  changerStatut(c: Commande, statut: StatutCommande): void {
    this.commandeService.changerStatut(c.id, statut).subscribe({
      next: () => this.charger(),
      error: () => this.erreur.set('Impossible de changer le statut de cette commande.'),
    });
  }

  variante(statut: StatutCommande): 'succes' | 'attente' | 'echec' | 'info' {
    if (statut === 'LIVREE' || statut === 'CONFIRMEE') return 'succes';
    if (statut === 'EN_ATTENTE_PAIEMENT' || statut === 'EN_PREPARATION') return 'attente';
    if (statut === 'ECHOUEE') return 'echec';
    return 'info';
  }

  exporterCsv(): void {
    fetch(this.commandeService.urlExport(), {
      headers: { Authorization: `Bearer ${this.authService.token}` },
    })
      .then((r) => r.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'commandes.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      })
      .catch(() => this.erreur.set("Échec de l'export CSV."));
  }
}
