# As'Soué Store — Frontend Angular

Frontend Angular 22 (standalone components, signaux, Reactive Forms,
Tailwind CSS) de l'API Spring Boot d'As'Soué, située dans le dossier voisin
`../backend`.

## Démarrer

```bash
npm install
npm start
```

Prérequis : Node.js >= 22.22.3 (exigence du CLI Angular 22).

L'application tourne sur http://localhost:4200. Elle n'utilise aucune URL
absolue : tous les appels partent vers `/api/...` (voir
`src/app/core/api-config.ts`) et le dev-server Angular les renvoie vers
`http://localhost:8080` grâce à `proxy.conf.json`. Le backend doit donc
simplement tourner sur 8080 ; il n'y a pas de question de CORS à régler,
puisque le navigateur ne voit qu'une seule origine.

En Docker, c'est Nginx (`nginx.conf`) qui joue ce rôle de proxy — voir le
`README.md` à la racine du projet.

## Comptes de test

Le backend ne "seed" que le compte ADMIN par défaut (voir `AdminSeeder`
côté backend). Créez ensuite :
1. Un compte CITOYEN via "Créer un compte" (inscription + code OTP visible
   dans les logs du backend, puisque l'envoi de SMS est simulé).
2. Des comptes ENTREPRISE depuis le back-office admin
   (`/admin/entreprises`) — dont, si besoin, le compte marqué "As'Soué
   elle-même" pour recevoir automatiquement les signalements de pneus.

## Structure

- `src/app/core` : modèles, services HTTP (un par domaine backend), garde
  d'authentification/rôle, intercepteur JWT.
- `src/app/shared` : navbar, footer, badge de statut réutilisable.
- `src/app/pages` : une page (ou un dossier de pages) par fonctionnalité,
  organisées comme les modules du backend (auth, produits, formations,
  signalements, panier/commandes, entreprise/marketplace, admin/*).

## Fonctionnalités couvertes

- Inscription citoyen + validation OTP, connexion (+ 2FA obligatoire pour
  ADMIN), mot de passe oublié.
- Boutique : catalogue public, fiche produit, panier local, commande +
  choix Orange Money / Moov Money.
- Signalements : upload photo (compressée côté backend), géolocalisation
  navigateur, adresse best-effort via Nominatim, suivi "mes signalements".
- Entreprise : fil marketplace trié par distance, prise en charge,
  marquage "collecté".
- Back-office admin : entreprises partenaires (avec recherche d'adresse
  OpenStreetMap), catalogue produits (CRUD), formations (création + liste
  des inscrits), signalements (changement de statut + export CSV),
  commandes (changement de statut + export CSV), historique des
  notifications SMS.

## Limite connue

L'API n'expose pas de route "mes prises en charge" pour une entreprise
(seul `/signalements/marketplace` liste les signalements EN_ATTENTE). Le
suivi des signalements pris en charge par l'entreprise (pour pouvoir les
marquer "collecté") est donc mémorisé côté navigateur (`localStorage`),
par compte entreprise — pas partagé entre appareils.
