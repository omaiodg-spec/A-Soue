import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CommandeService } from '../../core/services/commande.service';
import { BadgeStatut } from '../../shared/badge-statut/badge-statut';
import { Commande } from '../../core/models/commande.model';
import { LIBELLES_STATUT_COMMANDE, StatutCommande } from '../../core/models/enums';

@Component({
  selector: 'app-mes-commandes',
  imports: [DatePipe, DecimalPipe, BadgeStatut],
  templateUrl: './mes-commandes.html',
})
export class MesCommandes implements OnInit {
  private readonly commandeService = inject(CommandeService);

  commandes = signal<Commande[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  readonly libelles = LIBELLES_STATUT_COMMANDE;

  ngOnInit(): void {
    this.commandeService.mesCommandes().subscribe({
      next: (data) => {
        this.commandes.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger vos commandes.');
        this.chargement.set(false);
      },
    });
  }

  variante(statut: StatutCommande): 'succes' | 'attente' | 'echec' | 'info' {
    if (statut === 'LIVREE' || statut === 'CONFIRMEE') return 'succes';
    if (statut === 'EN_ATTENTE_PAIEMENT' || statut === 'EN_PREPARATION') return 'attente';
    if (statut === 'ECHOUEE') return 'echec';
    return 'info';
  }
}
