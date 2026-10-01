import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { Entreprise, EntrepriseCreate } from '../models/entreprise.model';

@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/entreprises`;

  lister(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(this.apiUrl);
  }

  creer(entreprise: EntrepriseCreate): Observable<Entreprise> {
    return this.http.post<Entreprise>(this.apiUrl, entreprise);
  }
}
