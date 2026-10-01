import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink],
  templateUrl: './topbar.html',
})
export class Topbar {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly titrePage = signal(this.lireTitreCourant());

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.titrePage.set(this.lireTitreCourant());
    });
  }

  protected get role(): string | null {
    return this.authService.role;
  }

  protected deconnecter(): void {
    this.authService.deconnecter();
    this.router.navigateByUrl('/connexion');
  }

  private lireTitreCourant(): string {
    let route: ActivatedRoute | null | undefined = this.route.root;
    let titre = 'Vue d\'ensemble';
    while (route) {
      titre = route.snapshot?.data?.['titre'] ?? titre;
      route = route.firstChild;
    }
    return titre;
  }
}
