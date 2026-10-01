import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PanierService } from '../../core/services/panier.service';

interface ElementNav {
  label: string;
  icon: string;
  link: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
})
export class Sidebar implements OnInit {
  private readonly authService = inject(AuthService);
  protected readonly panierService = inject(PanierService);

  // Reprend l'ordre/les icônes du sidebar de V2, en y ajoutant les fonctionnalités
  // propres à V1 (formations, panier) absentes de V2.
  protected readonly navigationCitoyen: ElementNav[] = [
    { label: "Vue d'ensemble", icon: '◈', link: '/dashboard' },
    { label: 'Signalements', icon: '⌁', link: '/signalements' },
    { label: 'Boutique', icon: '◇', link: '/boutique' },
    { label: 'Formations', icon: '🎓', link: '/formations' },
    { label: 'Panier', icon: '🛒', link: '/panier' },
    { label: 'Commandes', icon: '□', link: '/commandes' },
  ];

  protected readonly navigationEntreprise: ElementNav[] = [
    { label: "Vue d'ensemble", icon: '◈', link: '/dashboard' },
    { label: 'Fil des signalements', icon: '⌁', link: '/entreprise/marketplace' },
    { label: 'Commandes', icon: '□', link: '/commandes' },
  ];

  protected readonly nom = signal('Mon compte');
  protected readonly role = signal('');
  protected readonly initiales = signal('??');
  protected readonly estAdmin = computed(() => this.role() === 'ADMIN');
  protected readonly navigation = computed(() =>
    this.role() === 'ENTREPRISE' ? this.navigationEntreprise : this.navigationCitoyen
  );

  ngOnInit(): void {
    this.authService.monCompte().subscribe({
      next: (compte) => {
        this.nom.set(compte.nom);
        this.role.set(compte.role);
        this.initiales.set(this.calculerInitiales(compte.nom));
      },
      error: () => {}, // le sidebar reste utilisable même si l'appel échoue
    });
  }

  private calculerInitiales(nom: string): string {
    const parties = nom.trim().split(/\s+/);
    return parties.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || '??';
  }
}
