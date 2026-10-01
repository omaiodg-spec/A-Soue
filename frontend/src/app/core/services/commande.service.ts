import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { Commande, CommandeCreate } from '../models/commande.model';
import { StatutCommande } from '../models/enums';

@Injectable({ providedIn: 'root' })
export class CommandeService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/commandes`;

  creer(commande: CommandeCreate): Observable<Commande> {
    return this.http.post<Commande>(this.apiUrl, commande);
  }

  mesCommandes(): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.apiUrl}/mes-commandes`);
  }

  toutes(): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.apiUrl}/admin`);
  }

  changerStatut(id: number, statut: StatutCommande): Observable<Commande> {
    return this.http.patch<Commande>(`${this.apiUrl}/admin/${id}/statut`, { statut });
  }

  urlExport(): string {
    return `${this.apiUrl}/export`;
  }
}
