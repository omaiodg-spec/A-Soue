import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { Formation, FormationCreate, InscriptionFormation } from '../models/formation.model';
import { Utilisateur } from '../models/utilisateur.model';

@Injectable({ providedIn: 'root' })
export class FormationService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/formations`;

  lister(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.apiUrl);
  }

  obtenir(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/${id}`);
  }

  creer(formation: FormationCreate): Observable<Formation> {
    return this.http.post<Formation>(this.apiUrl, formation);
  }

  sInscrire(id: number): Observable<InscriptionFormation> {
    return this.http.post<InscriptionFormation>(`${this.apiUrl}/${id}/inscription`, {});
  }

  mesInscriptions(): Observable<InscriptionFormation[]> {
    return this.http.get<InscriptionFormation[]>(`${this.apiUrl}/mes-inscriptions`);
  }

  listerInscrits(id: number): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.apiUrl}/${id}/inscrits`);
  }
}
