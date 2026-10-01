import { Component, input } from '@angular/core';

// Petite pastille colorée réutilisable pour les statuts (signalement, commande...).
@Component({
  selector: 'app-badge-statut',
  templateUrl: './badge-statut.html',
})
export class BadgeStatut {
  readonly libelle = input.required<string>();
  // 'succes' | 'attente' | 'echec' | 'info'
  readonly variante = input<'succes' | 'attente' | 'echec' | 'info'>('info');
}
