import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { Signalement, SignalementCreate, SignalementMarketplace } from '../models/signalement.model';
import { StatutSignalement } from '../models/enums';

@Injectable({ providedIn: 'root' })
export class SignalementService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/signalements`;

  uploaderPhoto(fichier: File): Observable<{ photoUrl: string }> {
    const formData = new FormData();
    formData.append('fichier', fichier);
    return this.http.post<{ photoUrl: string }>(`${this.apiUrl}/photos`, formData);
  }

  creer(signalement: SignalementCreate): Observable<Signalement> {
    return this.http.post<Signalement>(this.apiUrl, signalement);
  }

  mesSignalements(): Observable<Signalement[]> {
    return this.http.get<Signalement[]>(`${this.apiUrl}/mes-signalements`);
  }

  fluxMarketplace(): Observable<SignalementMarketplace[]> {
    return this.http.get<SignalementMarketplace[]>(`${this.apiUrl}/marketplace`);
  }

  prendreEnCharge(id: number): Observable<Signalement> {
    return this.http.patch<Signalement>(`${this.apiUrl}/${id}/prendre-en-charge`, {});
  }

  marquerCollecte(id: number): Observable<Signalement> {
    return this.http.patch<Signalement>(`${this.apiUrl}/${id}/collecter`, {});
  }

  tousLesSignalements(): Observable<Signalement[]> {
    return this.http.get<Signalement[]>(`${this.apiUrl}/admin`);
  }

  changerStatut(id: number, statut: StatutSignalement): Observable<Signalement> {
    return this.http.patch<Signalement>(`${this.apiUrl}/admin/${id}/statut`, { statut });
  }

  urlPhoto(photoUrl: string): string {
    // photoUrl renvoyé par le backend est déjà un chemin absolu de type "/photos-signalements/xxx.jpg"
    return photoUrl.startsWith('http') ? photoUrl : `${API_BASE_URL}${photoUrl}`;
  }

  urlExport(): string {
    return `${this.apiUrl}/export`;
  }
}
