import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { Produit, ProduitCreate } from '../models/produit.model';

@Injectable({ providedIn: 'root' })
export class ProduitService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/produits`;

  lister(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.apiUrl);
  }

  obtenir(id: number): Observable<Produit> {
    return this.http.get<Produit>(`${this.apiUrl}/${id}`);
  }

  creer(produit: ProduitCreate): Observable<Produit> {
    return this.http.post<Produit>(this.apiUrl, produit);
  }

  modifier(id: number, produit: ProduitCreate): Observable<Produit> {
    return this.http.put<Produit>(`${this.apiUrl}/${id}`, produit);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
