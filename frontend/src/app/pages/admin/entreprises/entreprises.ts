import { AfterViewInit, Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import * as L from 'leaflet';
import { EntrepriseService } from '../../../core/services/entreprise.service';
import { GeocodageService } from '../../../core/services/geocodage.service';
import { Entreprise } from '../../../core/models/entreprise.model';
import { TypeDechet, LIBELLES_TYPE_DECHET } from '../../../core/models/enums';

@Component({
  selector: 'app-entreprises',
  imports: [ReactiveFormsModule],
  templateUrl: './entreprises.html',
})
export class Entreprises implements OnInit, AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly entrepriseService = inject(EntrepriseService);
  private readonly geocodageService = inject(GeocodageService);

  readonly typesDechet: TypeDechet[] = ['PNEU', 'PLASTIQUE', 'AUTRE'];
  readonly libellesTypeDechet = LIBELLES_TYPE_DECHET;

  entreprises = signal<Entreprise[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  creationEnCours = signal(false);
  rechercheAdresseEnCours = signal(false);
  localisationEnCours = signal(false);
  erreurLocalisation = signal<string | null>(null);

  @ViewChild('carteEntreprise') private carteElement?: ElementRef<HTMLDivElement>;
  private carte?: L.Map;
  private marqueur?: L.CircleMarker;

  form = this.fb.group({
    raisonSociale: ['', Validators.required],
    telephone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
    adresse: [''],
    latitude: [null as number | null],
    longitude: [null as number | null],
    typesDechetGeres: this.fb.array(
      this.typesDechet.map(() => this.fb.control(false))
    ),
    estAssoue: [false],
  });

  ngOnInit(): void {
    this.charger();
  }

  ngAfterViewInit(): void {
    if (!this.carteElement) return;
    this.carte = L.map(this.carteElement.nativeElement).setView([12.3714, -1.5197], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.carte);
    this.carte.on('click', (evenement: L.LeafletMouseEvent) => {
      this.definirPosition(evenement.latlng.lat, evenement.latlng.lng, true);
    });
    setTimeout(() => this.carte?.invalidateSize(), 0);
  }

  charger(): void {
    this.chargement.set(true);
    this.entrepriseService.lister().subscribe({
      next: (data) => {
        this.entreprises.set(data);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les entreprises.');
        this.chargement.set(false);
      },
    });
  }

  rechercherAdresse(): void {
    const adresse = this.form.getRawValue().adresse;
    if (!adresse) return;
    this.rechercheAdresseEnCours.set(true);
    this.geocodageService.rechercherCoordonnees(adresse).subscribe({
      next: (coords) => {
        this.rechercheAdresseEnCours.set(false);
        if (coords.latitude === null || coords.longitude === null) {
          this.erreur.set('Adresse introuvable via OpenStreetMap. Vérifiez la saisie.');
          return;
        }
        this.definirPosition(coords.latitude, coords.longitude, false, coords.adresseTrouvee);
        this.carte?.setView([coords.latitude, coords.longitude], 15);
      },
      error: () => {
        this.rechercheAdresseEnCours.set(false);
        this.erreur.set("Adresse introuvable via OpenStreetMap. Vérifiez la saisie.");
      },
    });
  }

  localiser(): void {
    this.erreurLocalisation.set(null);
    this.localisationEnCours.set(true);
    this.geocodageService.positionActuelle().subscribe({
      next: (position) => {
        this.localisationEnCours.set(false);
        this.definirPosition(position.coords.latitude, position.coords.longitude, true);
      },
      error: () => {
        this.localisationEnCours.set(false);
        this.erreurLocalisation.set('Impossible d’obtenir votre position. Vérifiez l’autorisation GPS.');
      },
    });
  }

  private definirPosition(latitude: number, longitude: number, rechercherAdresse: boolean, adresse?: string | null): void {
    this.form.patchValue({ latitude, longitude });
    const carte = this.carte;
    if (!carte) return;
    carte.setView([latitude, longitude], Math.max(carte.getZoom(), 15));
    this.marqueur?.remove();
    this.marqueur = L.circleMarker([latitude, longitude], {
      radius: 9,
      color: '#d7193f',
      fillColor: '#d7193f',
      fillOpacity: 0.85,
      weight: 3,
    }).addTo(carte);

    if (adresse) this.form.patchValue({ adresse });
    if (rechercherAdresse) {
      this.geocodageService.obtenirAdresse(latitude, longitude).subscribe({
        next: (resultat) => this.form.patchValue({ adresse: resultat.adresse || '' }),
      });
    }
  }

  creer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valeurs = this.form.getRawValue();
    const typesChoisis = this.typesDechet.filter((_, i) => valeurs.typesDechetGeres[i]);
    if (typesChoisis.length === 0) {
      this.erreur.set('Sélectionnez au moins un type de déchet géré.');
      return;
    }
    if (!valeurs.adresse && (valeurs.latitude === null || valeurs.longitude === null)) {
      this.erreur.set('Fournissez une adresse ou des coordonnées GPS.');
      return;
    }

    this.erreur.set(null);
    this.creationEnCours.set(true);
    this.entrepriseService.creer({
      raisonSociale: valeurs.raisonSociale!,
      telephone: valeurs.telephone!,
      motDePasse: valeurs.motDePasse!,
      adresse: valeurs.adresse || null,
      latitude: valeurs.latitude,
      longitude: valeurs.longitude,
      typesDechetGeres: typesChoisis,
      estAssoue: valeurs.estAssoue!,
    }).subscribe({
      next: () => {
        this.creationEnCours.set(false);
        this.form.reset({ estAssoue: false, typesDechetGeres: this.typesDechet.map(() => false) });
        this.marqueur?.remove();
        this.carte?.setView([12.3714, -1.5197], 12);
        this.charger();
      },
      error: (err) => {
        this.creationEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Impossible de créer cette entreprise.');
      },
    });
  }
}
