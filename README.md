<div align="center">
  <h1>♻️ As'Soué</h1>
  <p><strong>Plateforme innovante gestion des déchets recyclables à Ouagadougou. : les citoyens signalent des déchets géolocalisés et achètent des produits recyclés, les entreprises partenaires prennent en charge les collectes, As'Soué (ADMIN) supervise  l'ensemble.</strong></p>

  [![Angular](https://img.shields.io/badge/Angular-22-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.io/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
  [![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
</div>

---

## 📝 À propos du projet

**As'Soué** est une solution complète (Backend + Frontend) développée à deux, visant à faciliter la gestion des déchets recyclables à Ouagadougou. 

Le système connecte trois acteurs principaux :
- 🙋‍♂️ **Les citoyens** : signalent des déchets géolocalisés et peuvent acheter des produits recyclés.
- 🏢 **Les entreprises partenaires** : prennent en charge les collectes des déchets signalés.
- 🛡️ **L'Administration (As'Soué)** : supervise l'ensemble des opérations et gère la plateforme.

Ce projet a été réalisé en binôme dans une démarche d'apprentissage et de conception d'une architecture full-stack moderne.

---

## 🛠️ Stack Technique

### Frontend
- **Framework** : Angular 22 (Standalone components + Signals)
- **Styling** : Tailwind CSS
- **Serveur** : Nginx (avec reverse proxy pour l'API)

### Backend
- **Framework** : Spring Boot 3 (Java 17) - *6 ex-microservices fusionnés en un monolithe modulaire*
- **Base de données** : PostgreSQL (Docker) / H2 (en mémoire pour le dev)
- **Documentation API** : Swagger / OpenAPI

---

## 📂 Arborescence du projet

```bash
assoue/
├── docker-compose.yml       # Configuration de l'orchestration (Postgres + backend + frontend)
├── .env.example             # Modèle des variables d'environnement (secrets)
├── backend/                 # API Spring Boot
│   ├── Dockerfile
│   ├── pom.xml
│   ├── FONCTIONNEMENT.md    # Règles métier et documentation fonctionnelle
│   └── src/
└── frontend/                # Application Angular
    ├── Dockerfile           # Build multi-stage (Node + Nginx)
    ├── nginx.conf           # SPA routing + reverse proxy (/api → backend)
    ├── proxy.conf.json      # Reverse proxy pour le mode dev
    └── src/
```

---

## 🚀 Démarrage Rapide (avec Docker)

C'est la méthode recommandée pour lancer l'ensemble du projet en quelques minutes.

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/votre-nom-utilisateur/assoue.git
   cd assoue
   ```

2. **Configurer les variables d'environnement**
   ```bash
   cp .env.example .env
   # Pensez à éditer le fichier .env pour y mettre vos propres secrets en production
   ```

3. **Lancer les conteneurs**
   ```bash
   docker compose up --build
   ```
   *Note : Le premier démarrage peut prendre quelques minutes (téléchargement des images, build Maven et build Angular).*

### 🌐 Accès aux services

| Service | URL |
|---|---|
| 📱 **Application Frontend** | http://localhost:4200 |
| 🔌 **API (Swagger UI)** | http://localhost:8080/swagger-ui.html |
| nd **Base de données** | `localhost:5432` (base `assoue_db`) |

### 🔑 Compte Administrateur par défaut
Si aucun admin n'existe, le système en crée un au premier lancement :
- **Identifiant** : `admin@cif.bf` *(Utilisé dans le champ téléphone)*
- **Mot de passe** : `admin123`
> ⚠️ **Important** : À modifier immédiatement via "Mon compte" après la première connexion !

---

## 💻 Développement sans Docker (Local)

Vous pouvez lancer les services séparément pour le développement.

**Terminal 1 : Backend** (Utilise une base H2 en mémoire par défaut)
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 : Frontend** (Prérequis : Node.js ≥ 22.22.3)
```bash
cd frontend
npm install
npm start
```
*Le frontend sera accessible sur `http://localhost:4200` et les requêtes `/api/**` seront automatiquement redirigées vers `http://localhost:8080`.*

---

## 🔐 Sécurité & Variables d'environnement

Le fichier `.env` gère la configuration globale. **Trois secrets sont critiques en production :**
- `JWT_SECRET` : Signature des jetons d'authentification (min. 32 caractères).
- `DATA_ENCRYPTION_KEY` : Clé de chiffrement au repos (Nom et Téléphone). Indispensable pour la conformité CIL (Perdre cette clé = Perte des données).
- `PAYMENT_WEBHOOK_SECRET` : Signature HMAC pour valider les webhooks Orange/Moov Money.

---

## 📚 Documentation détaillée

Pour en savoir plus sur l'architecture et le fonctionnement interne :
- 📄 [Règles métier et sécurité](./backend/FONCTIONNEMENT.md)
- ⚙️ [Détails du Backend et API](./backend/README-backend.md)
- 🎨 [Détails du Frontend (Pages & Services)](./frontend/README-frontend.md)

---

## 👥 Auteurs / Contributeurs

Ce projet a été imaginé et développé en binôme par :

* **[Ton Prénom/Nom]** - *[Rôle : ex: Développeur Backend / Frontend]* - [@TonGithub](https://github.com/TonGithub)
* **[Prénom/Nom de ton binôme]** - *[Rôle : ex: Développeur Backend / Frontend]* - [@SonGithub](https://github.com/SonGithub)

---

<div align="center">
  <i>Si ce projet vous plaît, n'hésitez pas à laisser une ⭐ !</i>
</div>
