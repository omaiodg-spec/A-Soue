# Politique de sécurité

## Signaler une vulnérabilité

Merci de **ne pas** ouvrir une issue publique pour une faille de sécurité.

- Utilisez de préférence l'onglet **Security → Report a vulnerability** du dépôt GitHub (signalement privé), ou contactez directement les auteurs listés dans le [README](./README.md).
- Indiquez : la route ou le composant concerné, les étapes pour reproduire, l'impact estimé. Ne joignez aucune donnée personnelle réelle.

Nous accusons réception sous quelques jours et corrigeons en priorité les failles touchant l'authentification, les paiements et les données personnelles.

## Ce que le projet fait déjà

JWT stateless et BCrypt ; 2FA SMS obligatoire pour les ADMIN ; nom et téléphone chiffrés au repos (AES-256-GCM) ; webhook de paiement signé (HMAC-SHA256) et idempotent ; prix recalculés côté serveur ; garde-fou des secrets au démarrage (`APP_STRICT_SECRETS`) ; limitation de débit Nginx sur `/api/auth/` ; en-têtes de sécurité HTTP.

## Règles pour les contributeurs

- Ne commitez jamais de secret : `.env` est ignoré par Git, seul `.env.example` (placeholders) est versionné. Un secret commité doit être considéré comme compromis et **changé**, pas seulement supprimé.
- Les valeurs `CHANGE_MOI` et `admin123` sont publiques : elles ne servent qu'au développement local.
- Toute nouvelle route doit être protégée par rôle dans `SecurityConfig` et couverte par un test `*SecurityTest`.

## Avant la mise en production

Suivre [`docs/PRODUCTION-CHECKLIST.md`](./docs/PRODUCTION-CHECKLIST.md).
