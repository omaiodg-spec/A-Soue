import { AfterViewInit, Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import * as L from 'leaflet';
import { SignalementService } from '../../../core/services/signalement.service';
import { GeocodageService } from '../../../core/services/geocodage.service';
import { TypeDechet, LIBELLES_TYPE_DECHET } from '../../../core/models/enums';

@Component({
  selector: 'app-nouveau-signalement',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './nouveau-signalement.html',
})
export class NouveauSignalement implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly signalementService = inject(SignalementService);
  private readonly geocodageService = inject(GeocodageService);
  private readonly router = inject(Router);

  readonly typesDechet: TypeDechet[] = ['PNEU', 'PLASTIQUE', 'AUTRE'];
  readonly libellesTypeDechet = LIBELLES_TYPE_DECHET;

  form = this.fb.group({
    typeDechet: ['' as TypeDechet | '', Validators.required],
  });

  fichierPhoto: File | null = null;
  apercuPhoto = signal<string | null>(null);
  position = signal<{ latitude: number; longitude: number } | null>(null);
  adresseTrouvee = signal<string | null>(null);

  localisationEnCours = signal(false);
  erreurLocalisation = signal<string | null>(null);
  rechercheLocalite = signal('');
  rechercheEnCours = signal(false);

  @ViewChild('carte') private carteElement?: ElementRef<HTMLDivElement>;
  private carte?: L.Map;
  private marqueur?: L.CircleMarker;
  private readonly centreOuagadougou: L.LatLngExpression = [12.3714, -1.5197];

  photoUploadee = signal<string | null>(null);
  televersementEnCours = signal(false);

  soumissionEnCours = signal(false);
  erreur = signal<string | null>(null);
  numeroSuivi = signal<string | null>(null);

  ngAfterViewInit(): void {
    if (!this.carteElement) return;
    this.carte = L.map(this.carteElement.nativeElement).setView(this.centreOuagadougou, 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.carte);
    this.carte.on('click', (evenement: L.LeafletMouseEvent) => {
      this.definirPosition(evenement.latlng.lat, evenement.latlng.lng, true);
    });
    setTimeout(() => this.carte?.invalidateSize(), 0);
  }

  choisirPhoto(evenement: Event): void {
    const input = evenement.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) return;

    this.fichierPhoto = fichier;
    this.photoUploadee.set(null);
    const lecteur = new FileReader();
    lecteur.onload = () => this.apercuPhoto.set(lecteur.result as string);
    lecteur.readAsDataURL(fichier);
  }

  televerserPhoto(): void {
    if (!this.fichierPhoto) return;
    this.erreur.set(null);
    this.televersementEnCours.set(true);
    this.signalementService.uploaderPhoto(this.fichierPhoto).subscribe({
      next: (reponse) => {
        this.photoUploadee.set(reponse.photoUrl);
        this.televersementEnCours.set(false);
      },
      error: () => {
        this.erreur.set("Échec de l'envoi de la photo (image trop lourde ou format non supporté).");
        this.televersementEnCours.set(false);
      },
    });
  }

  localiser(): void {
    this.erreurLocalisation.set(null);
    this.localisationEnCours.set(true);
    this.geocodageService.positionActuelle().subscribe({
      next: (position) => {
        const coords = { latitude: position.coords.latitude, longitude: position.coords.longitude };
        this.definirPosition(coords.latitude, coords.longitude, true);
        this.localisationEnCours.set(false);
      },
      error: () => {
        this.localisationEnCours.set(false);
        this.erreurLocalisation.set("Impossible d'obtenir votre position. Vérifiez que la géolocalisation est autorisée.");
      },
    });
  }

  rechercherUneLocalite(): void {
    const localite = this.rechercheLocalite().trim();
    if (!localite) return;
    this.erreurLocalisation.set(null);
    this.rechercheEnCours.set(true);
    this.geocodageService.rechercherCoordonnees(localite).subscribe({
      next: (coordonnees) => {
        this.rechercheEnCours.set(false);
        if (coordonnees.latitude === null || coordonnees.longitude === null) {
          this.erreurLocalisation.set('Localité introuvable. Essayez une adresse plus précise.');
          return;
        }
        this.carte?.setView([coordonnees.latitude, coordonnees.longitude], 15);
        this.definirPosition(coordonnees.latitude, coordonnees.longitude, false, coordonnees.adresseTrouvee);
      },
      error: () => {
        this.rechercheEnCours.set(false);
        this.erreurLocalisation.set('Impossible de trouver cette localité.');
      },
    });
  }

  private definirPosition(latitude: number, longitude: number, rechercherAdresse: boolean, adresse?: string | null): void {
    this.position.set({ latitude, longitude });
    this.adresseTrouvee.set(adresse || null);
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

    if (rechercherAdresse) {
      this.geocodageService.obtenirAdresse(latitude, longitude).subscribe({
        next: (r) => this.adresseTrouvee.set(r.adresse || null),
        error: () => this.adresseTrouvee.set(null),
      });
    }
  }

  get peutSoumettre(): boolean {
    return !!this.photoUploadee() && !!this.position() && this.form.valid;
  }

  soumettre(): void {
    if (!this.peutSoumettre) {
      this.form.markAllAsTouched();
      return;
    }
    const pos = this.position()!;
    this.erreur.set(null);
    this.soumissionEnCours.set(true);

    this.signalementService.creer({
      typeDechet: this.form.getRawValue().typeDechet as TypeDechet,
      photoUrl: this.photoUploadee()!,
      latitude: pos.latitude,
      longitude: pos.longitude,
    }).subscribe({
      next: (reponse) => {
        this.soumissionEnCours.set(false);
        this.numeroSuivi.set(reponse.numeroSuivi);
      },
      error: (err) => {
        this.soumissionEnCours.set(false);
        this.erreur.set(err.error?.message ?? 'Une erreur est survenue lors de la création du signalement.');
      },
    });
  }

  nouveauSignalement(): void {
    this.fichierPhoto = null;
    this.apercuPhoto.set(null);
    this.photoUploadee.set(null);
    this.position.set(null);
    this.adresseTrouvee.set(null);
    this.rechercheLocalite.set('');
    this.marqueur?.remove();
    this.numeroSuivi.set(null);
    this.form.reset();
  }
}
