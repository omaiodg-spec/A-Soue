import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-inscription',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './inscription.html',
})
export class Inscription {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  chargement = signal(false);
  erreur = signal<string | null>(null);

  inscriptionForm = this.fb.group({
    nom: ['', Validators.required],
    telephone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
  });

  soumettreInscription(): void {
    if (this.inscriptionForm.invalid) {
      this.inscriptionForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    const { nom, telephone, motDePasse } = this.inscriptionForm.getRawValue();

    this.authService.inscrire(nom!, telephone!, motDePasse!).subscribe({
      next: () => {
        this.chargement.set(false);
        // La validation du numéro (OTP) se fait sur une page dédiée, comme côté V2.
        this.router.navigate(['/verification-otp'], { queryParams: { telephone } });
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err.error?.message ?? 'Ce numéro est peut-être déjà utilisé, ou les informations sont invalides.');
      },
    });
  }
}
