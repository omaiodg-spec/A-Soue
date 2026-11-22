import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Ajoute le JWT sur chaque requête vers l'API, et déconnecte proprement en cas de 401
// (token expiré ou invalide) en renvoyant vers l'écran de connexion.
// Exception : un 401 renvoyé par /auth/** ou par le changement de mot de passe (PUT /utilisateurs/moi)
// signifie « identifiants incorrects », pas « session expirée » -- sinon taper un mauvais mot de passe
// actuel dans « Mon compte » déconnectait l'utilisateur.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token;

  const requete = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  const estVerificationIdentifiants =
    req.url.includes('/auth/') || (req.method === 'PUT' && req.url.endsWith('/utilisateurs/moi'));

  return next(requete).pipe(
    catchError((erreur) => {
      if (erreur.status === 401 && authService.estConnecte && !estVerificationIdentifiants) {
        authService.deconnecter();
        router.navigate(['/connexion']);
      }
      return throwError(() => erreur);
    })
  );
};
