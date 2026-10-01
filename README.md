# As'Soué — projet complet (backend + frontend)

Plateforme de gestion des déchets recyclables à Ouagadougou : les citoyens
signalent des déchets géolocalisés et achètent des produits recyclés, les
entreprises partenaires prennent en charge les collectes, As'Soué (ADMIN)
supervise l'ensemble.

## Arborescence

```
assoue/
├── docker-compose.yml       # Postgres + backend + frontend
├── .env.example             # à copier en .env (secrets)
├── backend/                 # Spring Boot 3 / Java 17 (6 ex-microservices fusionnés)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── FONCTIONNEMENT.md    # documentation fonctionnelle détaillée
│   └── src/
└── frontend/                # Angular 22 (standalone + signals) + Tailwind
    ├── Dockerfile           # build Angular puis service par Nginx
    ├── nginx.conf           # SPA + reverse proxy /api → backend
    ├── proxy.conf.json      # équivalent pour "npm start" en dev
    └── src/
```

## Démarrage avec Docker (recommandé)

```bash
cp .env.example .env     # une seule fois, puis adapter les secrets
docker compose up --build
```

| Service | URL |
|---|---|
| Application | http://localhost:4200 |
| API (Swagger) | http://localhost:8080/swagger-ui.html |
| Postgres | localhost:5432 (base `assoue_db`) |

Premier démarrage : le build Maven + le build Angular prennent quelques
minutes. Les fois suivantes, Docker réutilise son cache.

Commandes utiles :

```bash
docker compose logs -f assoue-backend   # suivre les logs du backend
docker compose down                     # arrêter (les données sont conservées)
docker compose down -v                  # arrêter ET vider la base + les photos
docker compose up --build frontend      # reconstruire le frontend seul
```

## Compte administrateur par défaut

Créé automatiquement au premier démarrage, s'il n'existe aucun ADMIN :

- identifiant : `admin@cif.bf`
- mot de passe : `admin123`

À changer immédiatement via **Mon compte** après la première connexion.
L'identifiant de connexion du système est le champ « téléphone » ; pour le
compte admin on y place une adresse e-mail, ce qui est volontaire (aucun OTP
n'est envoyé à un numéro fictif).

## Comment le frontend parle au backend

Le frontend n'utilise aucune URL absolue : toutes les requêtes partent vers
`/api/...` sur **la même origine** que la page (`frontend/src/app/core/api-config.ts`).
La traduction vers le backend est faite :

- **en Docker** par Nginx (`frontend/nginx.conf`) : `/api/auth/connexion` →
  `http://assoue-backend:8080/auth/connexion` ;
- **en dev local** par le dev-server Angular (`frontend/proxy.conf.json`), actif
  automatiquement avec `npm start`.

Conséquences : aucun problème de CORS côté navigateur, et l'application
fonctionne telle quelle sur `localhost`, sur une IP du réseau local ou derrière
un nom de domaine — sans recompiler. Le port 8080 reste publié uniquement pour
Swagger et les tests Postman.

## Développement sans Docker

Deux terminaux :

```bash
# 1) Backend (H2 en mémoire par défaut, aucune base à installer)
cd backend && mvn spring-boot:run

# 2) Frontend
cd frontend && npm install && npm start     # http://localhost:4200
```

Le proxy Angular renvoie `/api/**` vers `http://localhost:8080`, donc le
comportement est identique à celui de Docker.

Prérequis frontend : **Node.js ≥ 22.22.3** (exigence du CLI Angular 22).

## Variables d'environnement

Voir `.env.example` pour la liste commentée. Les trois secrets à changer
impérativement avant toute mise en production :

- `JWT_SECRET` — signature des jetons (min. 32 caractères) ;
- `DATA_ENCRYPTION_KEY` — chiffrement au repos du nom et du téléphone
  (exigence CIL Burkina Faso) ; la perdre rend les données illisibles ;
- `PAYMENT_WEBHOOK_SECRET` — signature HMAC des webhooks Orange/Moov Money.

## Documentation

- `backend/FONCTIONNEMENT.md` — parcours fonctionnels, règles métier, sécurité
- `backend/README-backend.md` — détail du backend et de l'API
- `frontend/README-frontend.md` — détail des pages et services Angular
