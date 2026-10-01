import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Ajoute le JWT sur chaque requête vers l'API, et déconnecte proprement en cas de 401
// (token expiré ou invalide) en renvoyant vers l'écran de connexion.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token;

  const requete = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requete).pipe(
    catchError((erreur) => {
      if (erreur.status === 401 && authService.estConnecte) {
        authService.deconnecter();
        router.navigate(['/connexion']);
      }
      return throwError(() => erreur);
    })
  );
};
