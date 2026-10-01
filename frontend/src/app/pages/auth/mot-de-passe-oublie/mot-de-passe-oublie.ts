import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mot-de-passe-oublie',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './mot-de-passe-oublie.html',
})
export class MotDePasseOublie {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  etape = signal<1 | 2>(1);
  chargement = signal(false);
  erreur = signal<string | null>(null);

  demandeForm = this.fb.group({
    telephone: ['', Validators.required],
  });

  reinitialisationForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
    nouveauMotDePasse: ['', [Validators.required, Validators.minLength(8)]],
  });

  demanderCode(): void {
    if (this.demandeForm.invalid) {
      this.demandeForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    this.authService.demanderReinitialisation(this.demandeForm.getRawValue().telephone!).subscribe({
      next: () => {
        this.chargement.set(false);
        this.etape.set(2);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err.error?.message ?? "Une erreur est survenue, vérifiez le numéro saisi.");
      },
    });
  }

  reinitialiser(): void {
    if (this.reinitialisationForm.invalid) {
      this.reinitialisationForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    const telephone = this.demandeForm.getRawValue().telephone!;
    const { code, nouveauMotDePasse } = this.reinitialisationForm.getRawValue();

    this.authService.confirmerReinitialisation(telephone, code!, nouveauMotDePasse!).subscribe({
      next: () => {
        this.chargement.set(false);
        this.router.navigate(['/connexion']);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err.error?.message ?? 'Code invalide ou expiré.');
      },
    });
  }
}
