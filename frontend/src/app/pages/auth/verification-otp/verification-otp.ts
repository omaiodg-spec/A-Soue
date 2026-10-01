import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verification-otp',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verification-otp.html',
})
export class VerificationOtp {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly telephone = this.route.snapshot.queryParamMap.get('telephone') ?? '';
  protected readonly chargement = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly otpForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
  });

  protected soumettre(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    const code = this.otpForm.getRawValue().code!;

    this.authService.validerInscription(this.telephone, code).subscribe({
      next: () => this.router.navigateByUrl('/connexion'),
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err.error?.message ?? 'Code invalide, expiré, ou trop de tentatives.');
      },
    });
  }
}
