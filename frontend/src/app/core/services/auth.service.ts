import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api-config';
import { JwtResponse, Utilisateur } from '../models/utilisateur.model';

const CLE_TOKEN = 'assoue_token';
const CLE_USER = 'assoue_user';

interface SessionUtilisateur {
  userId: number;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/auth`;

  // Etat de session accessible partout dans l'app (signal, réactif).
  readonly utilisateurConnecte = signal<SessionUtilisateur | null>(this.lireSessionStockee());

  private lireSessionStockee(): SessionUtilisateur | null {
    const brut = localStorage.getItem(CLE_USER);
    return brut ? (JSON.parse(brut) as SessionUtilisateur) : null;
  }

  get token(): string | null {
    return localStorage.getItem(CLE_TOKEN);
  }

  get estConnecte(): boolean {
    return !!this.token;
  }

  get role(): string | null {
    return this.utilisateurConnecte()?.role ?? null;
  }

  inscrire(nom: string, telephone: string, motDePasse: string): Observable<Utilisateur> {
    return this.http.post<Utilisateur>(`${this.apiUrl}/inscription`, { nom, telephone, motDePasse });
  }

  validerInscription(telephone: string, code: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/valider-inscription`, { telephone, code });
  }

  connecter(telephone: string, motDePasse: string): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/connexion`, { telephone, motDePasse }).pipe(
      tap((reponse) => this.gererReponseAuth(reponse))
    );
  }

  verifierOtpConnexion(telephone: string, code: string): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/connexion/verifier-otp`, { telephone, code }).pipe(
      tap((reponse) => this.gererReponseAuth(reponse))
    );
  }

  demanderReinitialisation(telephone: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/mot-de-passe-oublie`, null, { params: { telephone } });
  }

  confirmerReinitialisation(telephone: string, code: string, nouveauMotDePasse: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reinitialiser-mot-de-passe`, { telephone, code, nouveauMotDePasse });
  }

  monCompte(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${API_BASE_URL}/utilisateurs/moi`);
  }

  mettreAJourMonCompte(payload: {
    motDePasseActuel: string;
    nom?: string;
    telephone?: string;
    nouveauMotDePasse?: string;
  }): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${API_BASE_URL}/utilisateurs/moi`, payload);
  }

  private gererReponseAuth(reponse: JwtResponse): void {
    if (reponse.token) {
      localStorage.setItem(CLE_TOKEN, reponse.token);
      const session: SessionUtilisateur = { userId: reponse.userId, role: reponse.role };
      localStorage.setItem(CLE_USER, JSON.stringify(session));
      this.utilisateurConnecte.set(session);
    }
  }

  deconnecter(): void {
    localStorage.removeItem(CLE_TOKEN);
    localStorage.removeItem(CLE_USER);
    this.utilisateurConnecte.set(null);
  }
}
