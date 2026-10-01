import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SignalementService } from '../../../core/services/signalement.service';
import { AuthService } from '../../../core/services/auth.service';
import { SignalementMarketplace } from '../../../core/models/signalement.model';
import { LIBELLES_STATUT_SIGNALEMENT, LIBELLES_TYPE_DECHET, TypeDechet } from '../../../core/models/enums';

interface PriseEnCharge {
  id: number;
  numeroSuivi: string;
  typeDechet: TypeDechet;
  collecte: boolean;
}

@Component({
  selector: 'app-marketplace',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './marketplace.html',
})
export class Marketplace implements OnInit {
  private readonly signalementService = inject(SignalementService);
  private readonly authService = inject(AuthService);

  signalements = signal<SignalementMarketplace[]>([]);
  prisesEnCharge = signal<PriseEnCharge[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  actionEnCoursId = signal<number | null>(null);
  readonly libellesType = LIBELLES_TYPE_DECHET;
  readonly libellesStatut = LIBELLES_STATUT_SIGNALEMENT;

  private get cleStockage(): string {
    return `assoue_prises_en_charge_${this.authService.utilisateurConnecte()?.userId ?? 'inconnu'}`;
  }

  ngOnInit(): void {
    const brut = localStorage.getItem(this.cleStockage);
    this.prisesEnCharge.set(brut ? JSON.parse(brut) : []);
    this.charger();
  }

  private sauvegarderPrisesEnCharge(): void {
    localStorage.setItem(this.cleStockage, JSON.stringify(this.prisesEnCharge()));
  }

  charger(): void {
    this.chargement.set(true);
    this.signalementService.fluxMarketplace().subscribe({
      next: (data) => {
        this.signalements.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger le fil des signalements.');
        this.chargement.set(false);
      },
    });
  }

  prendreEnCharge(s: SignalementMarketplace): void {
    this.actionEnCoursId.set(s.id);
    this.erreur.set(null);
    this.signalementService.prendreEnCharge(s.id).subscribe({
      next: () => {
        this.actionEnCoursId.set(null);
        this.prisesEnCharge.set([
          ...this.prisesEnCharge(),
          { id: s.id, numeroSuivi: s.numeroSuivi, typeDechet: s.typeDechet, collecte: false },
        ]);
        this.sauvegarderPrisesEnCharge();
        this.charger();
      },
      error: (err) => {
        this.actionEnCoursId.set(null);
        this.erreur.set(err.error?.message ?? "Ce signalement vient déjà d'être pris en charge par une autre entreprise.");
        this.charger();
      },
    });
  }

  estDejaPrisEnCharge(s: SignalementMarketplace): boolean {
    return s.statut === 'PRIS_EN_CHARGE';
  }

  marquerCollecteDepuisFlux(s: SignalementMarketplace): void {
    this.marquerCollecte({
      id: s.id,
      numeroSuivi: s.numeroSuivi,
      typeDechet: s.typeDechet,
      collecte: false,
    });
  }

  marquerCollecte(prise: PriseEnCharge): void {
    this.actionEnCoursId.set(prise.id);
    this.erreur.set(null);
    this.signalementService.marquerCollecte(prise.id).subscribe({
      next: () => {
        this.actionEnCoursId.set(null);
        this.prisesEnCharge.set(
          this.prisesEnCharge().map((p) => (p.id === prise.id ? { ...p, collecte: true } : p))
        );
        this.sauvegarderPrisesEnCharge();
        this.charger();
      },
      error: (err) => {
        this.actionEnCoursId.set(null);
        this.erreur.set(err.error?.message ?? 'Impossible de marquer ce signalement comme collecté.');
      },
    });
  }

  urlPhoto(url: string): string {
    return this.signalementService.urlPhoto(url);
  }
}
