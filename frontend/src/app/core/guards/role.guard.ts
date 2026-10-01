import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/enums';

// Utilisation dans les routes : { canActivate: [roleGuard(['ADMIN'])] }
export const roleGuard = (rolesAutorises: Role[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.estConnecte) {
      router.navigate(['/connexion']);
      return false;
    }
    if (!rolesAutorises.includes(authService.role as Role)) {
      router.navigate(['/dashboard']);
      return false;
    }
    return true;
  };
};
