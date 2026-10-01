import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';

export interface Coordonnees {
  latitude: number | null;
  longitude: number | null;
  adresseTrouvee: string | null;
}

@Injectable({ providedIn: 'root' })
export class GeocodageService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/geocodage`;

  obtenirAdresse(latitude: number, longitude: number): Observable<{ adresse: string }> {
    return this.http.get<{ adresse: string }>(`${this.apiUrl}/adresse`, { params: { latitude, longitude } });
  }

  rechercherCoordonnees(adresse: string): Observable<Coordonnees> {
    return this.http.get<Coordonnees>(`${this.apiUrl}/coordonnees`, { params: { adresse } });
  }

  positionActuelle(): Observable<GeolocationPosition> {
    return new Observable((observer) => {
      if (!navigator.geolocation) {
        observer.error(new Error('La géolocalisation n\'est pas disponible sur cet appareil.'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          observer.next(position);
          observer.complete();
        },
        (erreur) => observer.error(erreur),
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });
  }
}
