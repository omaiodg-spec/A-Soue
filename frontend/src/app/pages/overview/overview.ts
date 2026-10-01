import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ActivityList, ElementActivite } from '../../shared/activity-list/activity-list';
import { FeatureCard } from '../../shared/feature-card/feature-card';
import { StatCard } from '../../shared/stat-card/stat-card';
import { AuthService } from '../../core/services/auth.service';
import { SignalementService } from '../../core/services/signalement.service';
import { CommandeService } from '../../core/services/commande.service';
import { Commande } from '../../core/models/commande.model';
import { Signalement } from '../../core/models/signalement.model';

@Component({
  selector: 'app-overview',
  imports: [RouterLink, StatCard, FeatureCard, ActivityList],
  templateUrl: './overview.html',
})
export class Overview implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly signalementService = inject(SignalementService);
  private readonly commandeService = inject(CommandeService);

  protected readonly prenom = signal('Mon espace');
  protected readonly dateDuJour = this.formaterDate(new Date());

  protected readonly stats = [
    { value: '0', label: 'Signalements créés', tone: 'green' as const },
    { value: '0', label: 'Collectes terminées', tone: 'yellow' as const },
    { value: '0', label: 'Commandes en cours', tone: 'coral' as const },
  ];

  protected readonly activites = signal<ElementActivite[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  ngOnInit(): void {
    forkJoin({
      compte: this.authService.monCompte(),
      signalements: this.signalementService.mesSignalements(),
      commandes: this.commandeService.mesCommandes(),
    }).subscribe({
      next: ({ compte, signalements, commandes }) => {
        this.prenom.set(compte.nom.trim().split(/\s+/)[0]);
        this.stats[0].value = String(signalements.length);
        this.stats[1].value = String(signalements.filter((s) => s.statut === 'COLLECTE').length);
        this.stats[2].value = String(commandes.filter((c) => c.statut === 'CONFIRMEE' || c.statut === 'EN_PREPARATION').length);
        this.activites.set(this.construireActivites(signalements, commandes));
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les données de votre espace.');
        this.chargement.set(false);
      },
    });
  }

  private construireActivites(signalements: Signalement[], commandes: Commande[]): ElementActivite[] {
    const activites = [
      ...signalements.map((signalement) => ({
        date: signalement.dateCreation,
        activite: {
          icon: '⌁',
          tone: signalement.statut === 'COLLECTE' ? 'green' as const : 'orange' as const,
          titre: signalement.statut === 'COLLECTE' ? 'Signalement collecté' : 'Signalement créé',
          detail: `${signalement.numeroSuivi} · ${signalement.typeDechet}`,
          temps: this.formaterDateActivite(signalement.dateCreation),
        },
      })),
      ...commandes.map((commande) => ({
        date: commande.dateCreation,
        activite: {
          icon: '◇',
          tone: 'green' as const,
          titre: 'Commande enregistrée',
          detail: `Commande #${commande.id}`,
          temps: this.formaterDateActivite(commande.dateCreation),
        },
      })),
    ];

    return activites
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
      .slice(0, 5)
      .map(({ activite }) => activite);
  }

  private formaterDateActivite(dateIso: string): string {
    return new Date(dateIso).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  private formaterDate(date: Date): string {
    const jours = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];
    const mois = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
    return `${jours[date.getDay()]} ${date.getDate()} ${mois[date.getMonth()]} ${date.getFullYear()}`;
  }
}
