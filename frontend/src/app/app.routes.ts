import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

import { Connexion } from './pages/auth/connexion/connexion';
import { Inscription } from './pages/auth/inscription/inscription';
import { VerificationOtp } from './pages/auth/verification-otp/verification-otp';
import { MotDePasseOublie } from './pages/auth/mot-de-passe-oublie/mot-de-passe-oublie';

import { Layout } from './shared/layout/layout';
import { AdminLayout } from './shared/admin-layout/admin-layout';

import { Overview } from './pages/overview/overview';
import { ListeProduits } from './pages/produits/liste-produits/liste-produits';
import { DetailProduit } from './pages/produits/detail-produit/detail-produit';
import { ListeFormations } from './pages/formations/liste-formations/liste-formations';
import { DetailFormation } from './pages/formations/detail-formation/detail-formation';
import { NouveauSignalement } from './pages/signalements/nouveau-signalement/nouveau-signalement';
import { MesSignalements } from './pages/signalements/mes-signalements/mes-signalements';
import { Panier } from './pages/panier/panier';
import { MesCommandes } from './pages/mes-commandes/mes-commandes';
import { MonCompte } from './pages/mon-compte/mon-compte';
import { Marketplace } from './pages/entreprise/marketplace/marketplace';

import { Entreprises } from './pages/admin/entreprises/entreprises';
import { ProduitsAdmin } from './pages/admin/produits-admin/produits-admin';
import { FormationsAdmin } from './pages/admin/formations-admin/formations-admin';
import { SignalementsAdmin } from './pages/admin/signalements-admin/signalements-admin';
import { CommandesAdmin } from './pages/admin/commandes-admin/commandes-admin';
import { NotificationsAdmin } from './pages/admin/notifications-admin/notifications-admin';

import { NotFound } from './pages/not-found/not-found';

export const routes: Routes = [
  { path: '', redirectTo: 'connexion', pathMatch: 'full' },

  { path: 'connexion', component: Connexion },
  { path: 'inscription', component: Inscription },
  { path: 'verification-otp', component: VerificationOtp },
  { path: 'mot-de-passe-oublie', component: MotDePasseOublie },

  // Espace connecté (citoyen / entreprise) : sidebar + topbar, comme le Layout de V2.
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Overview, data: { titre: "Vue d'ensemble" } },

      { path: 'signalements', component: MesSignalements, data: { titre: 'Signalements' } },
      { path: 'signalements/nouveau', component: NouveauSignalement, data: { titre: 'Nouveau signalement' } },

      { path: 'boutique', component: ListeProduits, data: { titre: 'Boutique' } },
      { path: 'boutique/:id', component: DetailProduit, data: { titre: 'Produit' } },
      { path: 'formations', component: ListeFormations, data: { titre: 'Formations' } },
      { path: 'formations/:id', component: DetailFormation, data: { titre: 'Formation' } },

      { path: 'panier', component: Panier, data: { titre: 'Panier' } },
      { path: 'commandes', component: MesCommandes, data: { titre: 'Commandes' } },
      { path: 'mon-compte', component: MonCompte, data: { titre: 'Mon compte' } },

      { path: 'entreprise/marketplace', component: Marketplace, canActivate: [roleGuard(['ENTREPRISE'])], data: { titre: 'Fil des signalements' } },
    ],
  },

  // Back-office admin : sidebar sombre dédiée, comme AdminLayout de V2.
  {
    path: 'admin',
    component: AdminLayout,
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    children: [
      { path: '', redirectTo: 'produits', pathMatch: 'full' },
      { path: 'produits', component: ProduitsAdmin },
      { path: 'commandes', component: CommandesAdmin },
      { path: 'signalements', component: SignalementsAdmin },
      { path: 'entreprises', component: Entreprises },
      { path: 'formations', component: FormationsAdmin },
      { path: 'notifications', component: NotificationsAdmin },
    ],
  },

  { path: '**', component: NotFound },
];
