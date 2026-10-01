import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

type ModeConnexion = 'telephone' | 'email';

@Component({
  selector: 'app-connexion',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './connexion.html',
})
export class Connexion {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // otpRequis : cas d'un compte ADMIN, qui doit fournir un second facteur (code SMS) après le mot de passe.
  otpRequis = signal(false);
  chargement = signal(false);
  erreur = signal<string | null>(null);

  // Le backend n'a qu'un seul champ "telephone" servant d'identifiant : les deux modes
  // remplissent le même champ du formulaire, seuls le placeholder/type/validateurs changent à l'écran.
  protected readonly modeConnexion = signal<ModeConnexion>('telephone');

  connexionForm = this.fb.group({
    telephone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    motDePasse: ['', Validators.required],
  });

  otpForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
  });

  seConnecter(): void {
    if (this.connexionForm.invalid) {
      this.connexionForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    const { telephone, motDePasse } = this.connexionForm.getRawValue();

    this.authService.connecter(telephone!, motDePasse!).subscribe({
      next: (reponse) => {
        this.chargement.set(false);
        if (reponse.otpRequis) {
          this.otpRequis.set(true);
        } else {
          this.rediriger(reponse.role);
        }
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(
          err.error?.message ??
            (this.modeConnexion() === 'email'
              ? 'Email ou mot de passe incorrect, ou compte non validé.'
              : 'Téléphone ou mot de passe incorrect, ou compte non validé.')
        );
      },
    });
  }

  verifierOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }
    this.erreur.set(null);
    this.chargement.set(true);
    const telephone = this.connexionForm.getRawValue().telephone!;
    const code = this.otpForm.getRawValue().code!;

    this.authService.verifierOtpConnexion(telephone, code).subscribe({
      next: (reponse) => {
        this.chargement.set(false);
        this.rediriger(reponse.role);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err.error?.message ?? 'Code invalide ou expiré.');
      },
    });
  }

  protected readonly showPassword = signal(false);
  protected readonly annee = new Date().getFullYear();

  protected changerMode(mode: ModeConnexion): void {
    if (this.modeConnexion() === mode) {
      return;
    }
    this.modeConnexion.set(mode);
    this.connexionForm.controls.telephone.reset('');
    this.connexionForm.controls.telephone.setValidators([
      Validators.required,
      mode === 'email' ? Validators.email : Validators.pattern(/^\+?[0-9]{8,15}$/),
    ]);
    this.connexionForm.controls.telephone.updateValueAndValidity();
  }

  private rediriger(role: string): void {
    if (role === 'ADMIN') {
      this.router.navigate(['/admin']);
    } else if (role === 'ENTREPRISE') {
      this.router.navigate(['/entreprise/marketplace']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
