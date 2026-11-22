# Checklist de mise en production

À dérouler avant chaque déploiement visible par de vrais utilisateurs. Cochez chaque ligne ; une case non cochée = go refusé.

## 1. Bloquants fonctionnels (le projet ne peut pas partir sans)

- [ ] **Passerelle SMS réelle** branchée dans `SmsGatewayService` (aujourd'hui simulée).
- [ ] Le contenu des SMS (dont le code OTP) **n'est plus écrit dans les logs**.
- [ ] Le **2FA admin** fonctionne avec un vrai numéro : le compte `admin@cif.bf` n'est pas un numéro de téléphone, l'OTP ne pourrait pas lui être livré. Créer un ADMIN avec un vrai numéro, puis désactiver/renommer le compte seed.
- [ ] **Orange Money / Moov Money** intégrés : clé de signature réelle par opérateur dans `PAYMENT_WEBHOOK_SECRET`, vérification du montant et d'un horodatage dans le webhook.

## 2. Secrets et configuration

- [ ] `.env` créé avec `./scripts/init-env.sh` (ou secrets aléatoires ≥ 32 caractères), **aucun** `CHANGE_MOI`.
- [ ] `APP_STRICT_SECRETS=true`, `SWAGGER_ENABLED=false`, `H2_CONSOLE_ENABLED=false`.
- [ ] `DATA_ENCRYPTION_KEY` **sauvegardée à part** (coffre) : sans elle, noms et téléphones sont perdus.
- [ ] Mot de passe admin changé dans « Mon compte » ; `ADMIN_DEFAULT_PASSWORD` non réutilisé ailleurs.
- [ ] `GEOCODING_USER_AGENT` contient un vrai contact (exigence OpenStreetMap).
- [ ] Aucun secret n'a jamais été commité ; si c'est arrivé, il a été **remplacé** (pas seulement supprimé du dépôt).

## 3. Réseau et exposition

- [ ] HTTPS devant Nginx (certificat valide) ; redirection HTTP → HTTPS ; en-tête `Strict-Transport-Security` ajouté **après** avoir vérifié que tout le site fonctionne en HTTPS.
- [ ] Les ports 5432 (Postgres) et 8080 (backend) ne sont **pas** publiés sur Internet (par défaut, liés à `127.0.0.1`).
- [ ] Si Nginx est derrière un autre proxy/load balancer : configurer `real_ip` pour que la limite de débit `/api/auth/` compte les vraies IP clients.
- [ ] Politique CSP (`Content-Security-Policy`) testée sur un environnement de recette : autoriser `'self'`, les tuiles `https://*.tile.openstreetmap.org` (images) et Google Fonts (`fonts.googleapis.com` / `fonts.gstatic.com`) ou auto-héberger les polices.

## 4. Données et sauvegardes

- [ ] Sauvegarde automatique de PostgreSQL **et** du volume `assoue_photos_signalements`.
- [ ] Restauration testée au moins une fois (base + clé de chiffrement + photos).
- [ ] `spring.jpa.hibernate.ddl-auto=update` remplacé par des migrations versionnées (Flyway ou Liquibase) avant les évolutions de schéma en production.

## 5. Qualité et dépendances

- [ ] `cd backend && mvn test` : vert.
- [ ] Spring Boot mis à jour vers une branche **supportée** (la 3.3 ne reçoit plus de correctifs) ; dépendances backend et `npm audit` revus.
- [ ] Parcours manuel de bout en bout : inscription + OTP, connexion admin + 2FA, signalement PNEU et non-PNEU, commande + webhook signé (succès, échec, **rejeu**), formation, export CSV.

## 6. Déclencheurs de retour arrière (rollback)

Revenir à la version précédente si, dans l'heure suivant le déploiement :

- le taux d'erreurs 5xx dépasse 2 % sur `/api/auth/*` ou `/api/commandes` ;
- un utilisateur ne peut plus se connecter alors que son mot de passe est correct (suspicion de clé `DATA_ENCRYPTION_KEY` ou `JWT_SECRET` modifiée) ;
- des commandes restent `EN_ATTENTE_PAIEMENT` alors que le paiement est confirmé côté opérateur ;
- le démarrage du backend échoue après changement de configuration.

Procédure : `docker compose down` (**sans** `-v`, pour conserver base et photos) → rétablir l'image/le tag précédent et l'ancien `.env` → `docker compose up -d` → vérifier `/actuator/health` et une connexion.

## 7. Qui contacter

Compléter : responsable technique, responsable SMS/paiement (opérateurs), personne d'astreinte.
