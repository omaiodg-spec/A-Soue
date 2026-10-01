# As'Soué Store — Rapport technique du backend

> **Note (projet fusionné)** — ce backend fait désormais partie du projet
> complet As'Soué : le `docker-compose.yml` et le `.env` se trouvent **à la
> racine** (dossier parent), aux côtés du frontend Angular. Les commandes
> `cp .env.example .env` et `docker compose up --build` mentionnées plus bas
> sont donc à lancer depuis la racine, pas depuis ce dossier. Voir le
> `README.md` racine.

**Version du document :** accompagne la fusion des microservices en backend
unique, avec les évolutions marketplace, formations, upload photo et
géocodage OpenStreetMap.
**Périmètre :** backend Spring Boot **et** frontend Angular (dossier
`frontend/`), réunis dans ce même dépôt et prêts à fonctionner ensemble en
local (voir section « Démarrage rapide » ci-dessous).

Pour le détail fonctionnel (ce que fait chaque route, dans quel ordre, avec
quels rôles), voir **`FONCTIONNEMENT.md`** à la racine du projet — ce
document-ci couvre l'architecture, les choix techniques et l'exploitation.

---

## Démarrage rapide (backend + frontend en local)

**1. Backend** (API sur `http://localhost:8080`, base H2 en mémoire — aucune
installation de base de données nécessaire en dev) :

```bash
cd assoue-backend
mvn spring-boot:run
```

Swagger disponible sur `http://localhost:8080/swagger-ui.html`.

**2. Frontend** (Angular sur `http://localhost:4200`), dans un second
terminal :

```bash
cd assoue-backend/frontend
npm install
npm start
```

Le frontend appelle l'API via `environment.apiUrl` (voir
`frontend/src/environments/environment.ts`, déjà réglé sur
`http://localhost:8080`). Le backend autorise cette origine par défaut via
`CORS_ALLOWED_ORIGINS` (voir `.env.example` / `application.properties`) —
à adapter si le frontend est servi depuis un autre port ou domaine.

**Avec Docker** (backend + Postgres + frontend, tout conteneurisé) :

```bash
cp .env.example .env   # puis renseigner de vraies valeurs avant la prod
docker compose up --build
```

- Frontend : `http://localhost:4200` (Nginx sert le build Angular)
- Backend : `http://localhost:8080` (Swagger sur `/swagger-ui.html`)

Le frontend appelle le backend sur `http://localhost:8080` depuis le
navigateur de l'utilisateur (pas depuis le conteneur), donc ça fonctionne
sans configuration supplémentaire tant que le port 8080 du backend reste
publié sur l'hôte. Pour un déploiement sur un vrai domaine (pas
`localhost`), il faudra changer `apiUrl` dans
`frontend/src/environments/environment.ts` avant de reconstruire l'image
frontend, et mettre à jour `CORS_ALLOWED_ORIGINS` côté backend en
conséquence.

---

## Sommaire

1. [Résumé exécutif](#1-résumé-exécutif)
2. [Historique : de 6 microservices à un backend unique](#2-historique--de-6-microservices-à-un-backend-unique)
3. [Architecture générale](#3-architecture-générale)
4. [Stack technique](#4-stack-technique)
5. [Organisation du code](#5-organisation-du-code)
6. [Modèle de données](#6-modèle-de-données)
7. [Sécurité](#7-sécurité)
8. [Intégrations externes](#8-intégrations-externes)
9. [Référence des routes API](#9-référence-des-routes-api)
10. [Règles métier notables](#10-règles-métier-notables)
11. [Déploiement](#11-déploiement)
12. [Tests](#12-tests)
13. [Limites connues et travaux avant mise en production](#13-limites-connues-et-travaux-avant-mise-en-production)

---

## 1. Résumé exécutif

As'Soué Store est la plateforme numérique d'As'Soué (Ouagadougou, Burkina
Faso), reliant trois usages autour de la valorisation des déchets :

- des **citoyens** signalent des dépôts de déchets géolocalisés et achètent
  des produits recyclés issus de cette collecte ;
- des **entreprises** partenaires de collecte se disputent (ou reçoivent
  automatiquement, pour les pneus) les signalements dans leur périmètre
  d'activité ;
- **As'Soué** (rôle ADMIN) gère le catalogue, les comptes entreprises, la
  logistique des commandes, et forme la population.

Ce dépôt contient uniquement le **backend** : une API REST Spring Boot,
sans interface graphique. Il a démarré comme 6 microservices indépendants
(auth, catalogue, commande, notification, paiement, signalement), puis a
été fusionné en une seule application suite à une décision de
l'architecture cible (client-serveur classique, pas de microservices) —
voir section 2.

**État actuel :** fonctionnel de bout en bout avec des services externes
**simulés** là où l'intégration réelle dépend de tiers non encore fournis
(SMS, mobile money) — voir section 8 pour le détail de ce qui est réel et
ce qui est simulé.

---

## 2. Historique : de 6 microservices à un backend unique

Le projet a été initialement conçu en microservices (un service Spring
Boot par domaine, bases de données séparées, communication via Feign).
Cette architecture a été volontairement abandonnée pour les raisons
suivantes, constatées lors d'un audit du code :

- **Sécurité fragile aux frontières.** Les appels inter-services
  reposaient sur des routes HTTP `permitAll()` commentées "appel interne
  de confiance" — en réalité accessibles à quiconque sur Internet
  connaissant l'URL (fuite de données personnelles, confirmation de
  paiement falsifiable, etc.).
- **Complexité disproportionnée pour le besoin réel.** Le cahier des
  charges prévoit une architecture client-serveur classique (un frontend,
  un backend) — les microservices n'apportaient ni scalabilité
  indépendante utile à ce stade, ni isolation d'équipe (une seule équipe
  de développement).
- **Duplication de code.** Sécurité JWT, gestion d'erreurs, enums
  métier (`TypeDechet`, `Operateur`) étaient dupliqués à l'identique dans
  chaque service "pour rester autonomes", sans bénéfice réel puisque
  déployés ensemble de toute façon.

La fusion a consisté à :
1. Copier les 6 packages Java dans un seul projet Maven.
2. Consolider en un package `common/` tout ce qui était dupliqué (sécurité
   JWT, gestion d'erreurs, enums, documentation Swagger).
3. Remplacer les clients Feign par de l'injection Spring directe
   (`@Autowired`) — un appel qui traversait le réseau devient un simple
   appel de méthode Java.
4. Supprimer les routes HTTP qui n'existaient que pour ces appels internes
   (`/utilisateurs/**`, `/entreprises/par-type`, `/commandes/internal/**`,
   `/notifications/sms`, `/paiements/initier`) — la faille de sécurité
   correspondante disparaît avec elles, plutôt que d'être colmatée.
5. Résoudre le seul cycle de dépendances introduit par la fusion
   (`CommandeService` ↔ `PaiementService`) via un évènement Spring
   (`PaiementConfirmeEvent`) plutôt qu'un appel direct.

---

## 3. Architecture générale

```
                      ┌──────────────────────┐
                      │      Frontend         │   (projet séparé,
                      │  (web / mobile)        │    non couvert ici)
                      └───────────┬──────────┘
                                  │ HTTPS / JSON
                                  ▼
                      ┌──────────────────────┐
                      │   Backend Spring Boot │
                      │     (ce dépôt)         │
                      │                        │
                      │  auth · catalogue      │
                      │  commande · paiement   │
                      │  notification          │
                      │  signalement           │
                      │  formation · common    │
                      └───────────┬──────────┘
                                  │ JDBC
                                  ▼
                      ┌──────────────────────┐
                      │      PostgreSQL        │
                      │   (H2 en dev/tests)    │
                      └──────────────────────┘

  Services externes appelés par le backend :
  - OpenStreetMap / Nominatim  (géocodage, réel et actif)
  - Orange Money / Moov Money  (paiement, simulé pour l'instant)
  - Passerelle SMS             (notifications, simulée pour l'instant)
```

Un seul processus, une seule base de données, un seul port HTTP (`8080`).
Les "modules" (`auth`, `catalogue`, etc.) sont des packages Java, pas des
services déployables séparément — la frontière entre eux est une
convention de code (chacun a ses propres `controller/service/repository/
model/dto/mapper/exception`), pas une frontière réseau.

---

## 4. Stack technique

| Composant          | Choix                                   |
|---------------------|------------------------------------------|
| Langage / runtime    | Java 17                                  |
| Framework            | Spring Boot 3.3.4                        |
| Sécurité             | Spring Security 6 (JWT stateless, BCrypt) |
| Persistance          | Spring Data JPA / Hibernate               |
| Base de données      | PostgreSQL (prod), H2 en mémoire (dev/tests) |
| Build                | Maven                                    |
| Documentation API    | springdoc-openapi (Swagger UI)           |
| Tests                | JUnit 5, Mockito, AssertJ, Spring MockMvc |
| Conteneurisation     | Docker (multi-stage build), Docker Compose |
| Génération de JWT    | jjwt (io.jsonwebtoken)                    |
| Géocodage            | Nominatim (OpenStreetMap), via `RestTemplate` |

Aucune dépendance à un cloud provider spécifique ; déployable sur
n'importe quel serveur Linux avec Docker, ou directement avec un JRE 17.

---

## 5. Organisation du code

```
src/main/java/bf/formation/assoue/
├── AssoueApplication.java        Point d'entree unique
├── auth/                         Comptes, roles, entreprises, OTP, 2FA
├── catalogue/                    Produits recycles (CRUD, consultation publique)
├── commande/                     Panier, commandes, integration paiement
├── paiement/                     Transactions mobile money (simule), webhook signe
├── notification/                 Envoi de SMS (simule), historique
├── signalement/                  Signalements geolocalises, marketplace, upload photo
├── formation/                    Formations As'Soue, inscriptions
└── common/                       Transversal :
    ├── config/                     SecurityConfig, OpenApiConfig, RestClientConfig
    ├── security/                   JwtService, JwtAuthenticationFilter, CurrentUser
    ├── exception/                  GlobalExceptionHandler, ErrorResponse
    ├── model/                      Enums partages (TypeDechet, Operateur)
    ├── event/                      PaiementConfirmeEvent
    └── geocoding/                  GeocodingService (OpenStreetMap/Nominatim)
```

Chaque module métier suit la même structure interne : `controller/`
(HTTP), `service/` (logique métier), `repository/` (accès données),
`model/` (entités JPA), `dto/` (contrats API), `mapper/` (entité ↔ DTO),
`exception/` (erreurs spécifiques au module).

**Règle de dépendance :** un module peut appeler le `service` d'un autre
module directement (ex. `SignalementService` appelle `EntrepriseService`
et `SmsGatewayService`) — c'est le remplacement direct des anciens appels
Feign. Il n'y a pas de découplage supplémentaire (pas d'interface,
pas d'event pour la majorité des cas) sauf là où un cycle de dépendances
l'imposait (`PaiementConfirmeEvent`, voir section 2).

---

## 6. Modèle de données

Voir le diagramme de classes fourni séparément (`diagramme_classes.pdf`)
pour le détail complet des attributs et relations. Résumé des entités
principales :

| Entité | Rôle |
|---|---|
| `Utilisateur` | Compte (CITOYEN / ENTREPRISE / ADMIN), nom et téléphone chiffrés |
| `ProfilEntreprise` | Extension 1-1 de `Utilisateur` pour le rôle ENTREPRISE : localisation, types de déchets gérés, marqueur `estAssoue` |
| `OtpCode` | Codes à usage unique (inscription, reset mdp, 2FA admin) |
| `Produit` | Catalogue de produits recyclés |
| `Commande` / `LigneCommande` | Panier validé, lignes figées au prix du moment |
| `Transaction` | Paiement mobile money associé à une commande |
| `Signalement` | Déclaration géolocalisée d'un dépôt de déchets, avec adresse déduite (OpenStreetMap) |
| `NotificationLog` | Historique des SMS envoyés (simulés) |
| `Formation` / `InscriptionFormation` | Formations proposées par As'Soué et leurs inscrits |

**Chiffrement et hachage (table `utilisateurs`) :** `nom` et `telephone`
sont chiffrés au repos (AES-256-GCM). Comme ce chiffrement est non
déterministe, une colonne séparée `telephone_hash` (HMAC-SHA256,
déterministe) porte la contrainte d'unicité et sert à toutes les
recherches — jamais le texte en clair.

---

## 7. Sécurité

| Mécanisme | Détail |
|---|---|
| Authentification | JWT stateless, signé HMAC-SHA256 (`jwt.secret`), validé par `common.security.JwtAuthenticationFilter` sur toute route non explicitement publique |
| Mots de passe | BCrypt, salt factor 10 |
| 2FA | Obligatoire pour tout compte ADMIN : `/auth/connexion` renvoie `otpRequis: true` sans token, le vrai JWT n'est délivré qu'après `/auth/connexion/verifier-otp` |
| Anti brute-force OTP | Un code est définitivement invalidé après 5 tentatives incorrectes |
| Données personnelles | `nom`/`telephone` chiffrés (AES-256-GCM) ; recherche via `telephone_hash` (HMAC-SHA256), jamais en clair (voir section 6) |
| Webhook paiement | `POST /paiements/webhook` : seule route de paiement encore publique, protégée par vérification de signature HMAC (`X-Webhook-Signature`) sur le corps brut de la requête, calculée avant toute désérialisation JSON |
| Autorisation | Rôles CITOYEN / ENTREPRISE / ADMIN vérifiés par `SecurityConfig` (matchers Spring Security), voir section 9 pour le détail par route |
| Photos | Servies en statique sans authentification (`GET /photos-signalements/**`) — nécessaire pour un usage direct en balise `<img>`, qui ne porte pas de jeton JWT |
| Géocodage | Les deux routes `/geocodage/**` nécessitent d'être connecté, pour éviter qu'un tiers utilise le backend comme relais anonyme vers l'API gratuite d'OpenStreetMap |

**Ce qui a disparu avec la fusion (section 2) :** les anciennes routes
"internes" entre microservices n'existent plus du tout — il n'y a donc
plus de surface d'attaque à cet endroit, plutôt qu'une surface protégée
par une clé partagée.

---

## 8. Intégrations externes

| Service | État | Détail |
|---|---|---|
| **OpenStreetMap (Nominatim)** | ✅ **Réel, actif** | Géocodage inverse (coordonnées → adresse, automatique sur signalements/entreprises) et direct (adresse → coordonnées, pour la création d'entreprise). Instance publique gratuite, limitée à 1 requête/seconde (respectée côté backend). Voir `FONCTIONNEMENT.md` section 14 pour le détail. |
| **Orange Money / Moov Money** | ⏳ **Simulé** | `PaiementService.initier()` crée une transaction `INITIEE` avec une référence `SIM-xxxxxxxx`. La confirmation passe par `POST /paiements/webhook`, dont la signature HMAC est déjà vérifiée — il ne manque que le branchement sur les vraies clés API des opérateurs. |
| **Passerelle SMS** | ⏳ **Simulé** | `SmsGatewayService.envoyer()` enregistre le SMS comme "envoyé avec succès" dans `NotificationLog` sans réellement le transmettre à un opérateur télécom. À remplacer par un vrai fournisseur (ex. Twilio, ou un agrégateur local) avant la mise en production. |
| **Stockage des photos** | ✅ **Réel, sur disque** | `PhotoStorageService` compresse (max 800px) et stocke les photos de signalement sur le disque du serveur (volume Docker persistant), pas en base de données. Alternatives pour un volume important : MinIO auto-hébergé, ou un fournisseur S3-compatible (voir `FONCTIONNEMENT.md` section 11). |

---

## 9. Référence des routes API

Documentation interactive complète disponible sur `/swagger-ui.html` une
fois l'application lancée. Résumé par module :

### auth (`/auth`, `/entreprises`)
| Route | Rôle requis |
|---|---|
| `POST /auth/inscription` | public |
| `POST /auth/valider-inscription` | public |
| `POST /auth/connexion` | public |
| `POST /auth/connexion/verifier-otp` | public (2FA admin) |
| `POST /auth/mot-de-passe-oublie` | public |
| `POST /auth/reinitialiser-mot-de-passe` | public |
| `POST /entreprises` | ADMIN |
| `GET /entreprises` | ADMIN |

### catalogue (`/produits`)
| Route | Rôle requis |
|---|---|
| `GET /produits`, `GET /produits/{id}` | public |
| `POST /produits`, `PUT /produits/{id}`, `DELETE /produits/{id}` | ADMIN |

### commande (`/commandes`)
| Route | Rôle requis |
|---|---|
| `POST /commandes` | connecté |
| `GET /commandes/mes-commandes` | connecté |
| `GET /commandes/admin`, `PATCH /commandes/admin/{id}/statut`, `GET /commandes/export` | ADMIN |

### paiement (`/paiements`)
| Route | Rôle requis |
|---|---|
| `POST /paiements/webhook` | public + signature HMAC obligatoire |
| `GET /paiements/{id}` | connecté |

### signalement (`/signalements`)
| Route | Rôle requis |
|---|---|
| `POST /signalements/photos` | connecté |
| `POST /signalements` | connecté (CITOYEN ou ENTREPRISE) |
| `GET /signalements/mes-signalements` | connecté |
| `GET /signalements/marketplace`, `PATCH /signalements/{id}/prendre-en-charge`, `PATCH /signalements/{id}/collecter` | ENTREPRISE |
| `GET /signalements/admin`, `PATCH /signalements/admin/{id}/statut`, `GET /signalements/export` | ADMIN |

### formation (`/formations`)
| Route | Rôle requis |
|---|---|
| `GET /formations`, `GET /formations/{id}` | public |
| `POST /formations` | ADMIN |
| `POST /formations/{id}/inscription`, `GET /formations/mes-inscriptions` | connecté |
| `GET /formations/{id}/inscrits` | ADMIN |

### notification (`/notifications`)
| Route | Rôle requis |
|---|---|
| `GET /notifications` | ADMIN |

### geocodage (`/geocodage`)
| Route | Rôle requis |
|---|---|
| `GET /geocodage/adresse`, `GET /geocodage/coordonnees` | connecté |

---

## 10. Règles métier notables

- **Prix et disponibilité toujours recalculés côté serveur** au moment
  d'une commande — jamais les valeurs envoyées par le client.
- **Signalements PNEU** rattachés automatiquement au compte marqué
  `estAssoue = true` (un seul compte peut porter ce marqueur) — pas de
  mise en concurrence marketplace pour ce type.
- **Autres types de déchets** : toutes les entreprises du bon type sont
  informées par SMS, les plus proches en premier (tri par distance,
  formule de Haversine) ; premier arrivé, premier servi pour la prise en
  charge.
- **Comptes ENTREPRISE** créés exclusivement par un ADMIN — aucune
  auto-inscription possible pour ce rôle (à la différence de CITOYEN).
- **Formations** : inscription unique par utilisateur et par formation,
  capacité optionnelle (illimitée si non renseignée).

Le détail complet, avec le raisonnement pas-à-pas de chaque flux, est
dans `FONCTIONNEMENT.md`.

---

## 11. Déploiement

### Avec Docker (recommandé)
```bash
cp .env.example .env      # puis remplacer toutes les valeurs CHANGE_MOI
docker compose up --build
```
Démarre deux conteneurs : `postgres` (base persistante, volume
`assoue_postgres_data`) et `assoue-backend` (l'application, volume
`assoue_photos_signalements` pour les photos). API sur
`http://localhost:8080`, Swagger sur `/swagger-ui.html`.

### Sans Docker (développement rapide)
```bash
mvn spring-boot:run
```
Utilise H2 en mémoire — aucune variable d'environnement requise, données
perdues à chaque redémarrage.

### Variables d'environnement (`.env`)
| Variable | Rôle |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Identifiants de la base Postgres |
| `JWT_SECRET` | Secret de signature des JWT (32+ caractères) |
| `DATA_ENCRYPTION_KEY` | Clé de chiffrement des données personnelles — **la perdre rend les données irrécupérables** |
| `PAYMENT_WEBHOOK_SECRET` | Secret HMAC du webhook opérateur mobile money |
| `GEOCODING_USER_AGENT` | Identifie l'application auprès de Nominatim (obligatoire par leur politique d'usage) |
| `H2_CONSOLE_ENABLED` | `false` par défaut — ne jamais activer en production |

**Avant toute mise en production réelle :** générer des valeurs aléatoires
fortes pour `JWT_SECRET`, `DATA_ENCRYPTION_KEY` et `PAYMENT_WEBHOOK_SECRET`
(ne jamais garder les valeurs `CHANGE_MOI` fournies en exemple).

---

## 12. Tests

```bash
mvn test
```
Utilise un profil `test` dédié (H2 en mémoire propre à chaque exécution,
pas besoin de Postgres ni de Docker).

- **Tests unitaires** (`*ServiceTest.java`) : logique métier isolée,
  toutes les dépendances externes mockées (repositories, autres services,
  `RestTemplate` pour le géocodage, encodeur de mot de passe).
- **Tests d'intégration** (`*IntegrationTest.java`, `*SecurityTest.java`) :
  contrôleur + service + repository + vraie base H2 via `MockMvc`, JWT
  signés par `common.security.JwtTestUtils`. Les appels sortants vers
  Nominatim y sont systématiquement mockés (`@MockBean GeocodingService`)
  pour ne jamais dépendre d'un accès réseau réel pendant les tests.

---

## 13. Limites connues et travaux avant mise en production

- **SMS et mobile money simulés** (section 8) — intégration réelle à
  brancher dès que les accès sont fournis par les opérateurs.
- **Limite Nominatim à 1 req/s** : suffisant pour un usage modéré, à
  remplacer par un fournisseur payant (LocationIQ, Geoapify, Mapbox) si le
  volume de créations de signalements/entreprises devient important, ou
  si l'application tourne sur plusieurs instances en parallèle.
- **Pas de notification de fin de collecte** pour le citoyen (il doit
  consulter son historique) — à ajouter si souhaité dans
  `SignalementService.marquerCollecte()`.
- **Pas de notification d'échec de paiement** — à ajouter dans
  `CommandeService.notifierResultatPaiement()` si souhaité.
- **Suivi logistique des commandes** (`EN_PREPARATION`, `LIVREE`) entièrement
  manuel, géré par l'ADMIN — aucune automatisation prévue à ce stade.
