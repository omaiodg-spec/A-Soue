# Comment fonctionne As'Soué Store — guide pas à pas

Ce fichier explique, fonctionnalité par fonctionnalité, **ce qui se passe
réellement dans le code** quand quelqu'un utilise l'application. L'objectif :
que tu puisses le lire et te dire "oui c'est la logique attendue" ou "non, il
manque une étape / ça devrait se passer autrement" — et corriger.

Trois rôles existent : **CITOYEN** (auto-inscription libre, grand public),
**ENTREPRISE** (partenaire de collecte, compte créé **uniquement par un
ADMIN** — jamais d'auto-inscription pour ce rôle), **ADMIN** (équipe
As'Soué, back-office).

---

## 1. Créer un compte

**Point important à bien distinguer :**
- **CITOYEN** : n'importe qui peut créer son propre compte librement via
  `POST /auth/inscription` (nom, téléphone, mot de passe) — pas
  d'autorisation préalable nécessaire.
- **ENTREPRISE** : impossible de s'auto-inscrire. Seul un ADMIN peut créer
  ce type de compte, via `POST /entreprises`. Un CITOYEN ou une ENTREPRISE
  qui tente cet appel reçoit un 403 (refusé).

### Inscription CITOYEN, étape par étape
1. `POST /auth/inscription` avec nom, téléphone, mot de passe.
2. Vérification que ce numéro n'est pas déjà utilisé (via un hachage du
   téléphone, jamais le numéro en clair — voir section Sécurité plus bas).
3. Le compte est créé avec le rôle CITOYEN, mais **`telephoneVerifie =
   false`** — impossible de se connecter tant que l'étape 4 n'est pas faite.
4. Un code à 6 chiffres est "envoyé par SMS" (simulé, voir section 12),
   valable 5 minutes, 5 tentatives max.
5. `POST /auth/valider-inscription` (téléphone + code) : si correct,
   `telephoneVerifie` passe à `true` et le compte devient utilisable.

**Ce qui doit te sembler logique :** un compte CITOYEN existe en base
*avant* la validation du téléphone, mais reste inutilisable tant que ce
n'est pas fait. Une ENTREPRISE, elle, est créée **directement utilisable**
(pas d'étape OTP) puisque l'ADMIN l'a déjà vérifiée en amont.

**Erreurs possibles :** téléphone déjà utilisé → 409. Champs invalides →
400. Code OTP faux, expiré, ou 5 tentatives dépassées → 400.

---

## 2. Se connecter (CITOYEN ou ENTREPRISE)

1. `POST /auth/connexion` avec téléphone + mot de passe.
2. Vérifications dans l'ordre : le compte existe → le mot de passe
   correspond → le téléphone a été validé.
3. Si tout est bon **et que ce n'est pas un compte ADMIN** : un token JWT
   est renvoyé directement.

La moindre étape manquante renvoie la même erreur générique (401), jamais
un 404, pour ne pas révéler si un numéro existe en base.

---

## 3. Se connecter en tant qu'ADMIN (2FA obligatoire)

Le mot de passe seul ne suffit jamais pour un ADMIN.

1. `POST /auth/connexion` — si le compte a le rôle ADMIN, réponse
   `otpRequis: true`, **`token: null`**, un code SMS est envoyé.
2. `POST /auth/connexion/verifier-otp` (téléphone + code) : si correct,
   **c'est seulement là** que le vrai JWT est délivré.

---

## 4. Mot de passe oublié

`POST /auth/mot-de-passe-oublie` (envoie un OTP) puis
`POST /auth/reinitialiser-mot-de-passe` (téléphone + code + nouveau mot de
passe). Même mécanisme que l'inscription, pas de facteur supplémentaire.

---

## 5. Créer un compte ENTREPRISE (réservé à l'ADMIN)

1. `POST /entreprises` — **ADMIN uniquement**, formulaire avec raison
   sociale, téléphone, mot de passe initial, types de déchets gérés (`PNEU`,
   `PLASTIQUE`, `AUTRE`), et la localisation du siège — **soit des
   coordonnées GPS, soit une adresse tapée** (au moins l'un des deux ; si
   seule l'adresse est donnée, elle est convertie en coordonnées via
   OpenStreetMap, voir section 14).
2. Compte créé **directement vérifié** (pas d'OTP).
3. **Cas particulier : le compte As'Soué elle-même.** Un champ `estAssoue`
   (à cocher une seule fois, sur un seul compte) marque quelle entreprise
   *est* As'Soué. C'est ce compte qui recevra automatiquement tous les
   signalements de type PNEU (voir section 6). Si personne ne coche jamais
   ce champ, les signalements PNEU restent simplement en attente — donc
   **il faut penser à créer ce compte dès la mise en route**, sinon les
   pneus n'iront nulle part automatiquement.

---

## 6. Faire un signalement (CITOYEN **ou** ENTREPRISE)

**N'importe quel compte connecté peut signaler**, pas seulement les
citoyens — une entreprise qui repère un dépôt sauvage peut aussi le
signaler comme n'importe quel utilisateur, la route ne filtre pas le rôle.

### Comment la localisation fonctionne (mis à jour : intégration OpenStreetMap)
Il n'y a toujours aucune intégration Street View ni Google Maps. Le backend
reçoit `latitude`/`longitude` envoyés par l'application (GPS du téléphone ou
carte côté frontend), **et convertit maintenant automatiquement ces
coordonnées en adresse lisible** via l'API **Nominatim d'OpenStreetMap**
(ex. "Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso"). Cette adresse est
stockée sur le signalement et renvoyée dans les réponses API — en plus des
coordonnées brutes, pas à la place.

Ce geocodage est **best-effort** : si Nominatim est indisponible ou lent, le
signalement est quand même créé normalement, juste sans adresse lisible
(`adresse: null`). Voir la section 14 plus bas pour le détail technique de
cette intégration (les deux sens : coordonnées → adresse, et adresse →
coordonnées).

### Deux traitements différents selon le type de déchet

**Cas 1 — Type `PNEU` : rattachement automatique, pas de concurrence**
1. Le signalement est créé.
2. Le serveur cherche immédiatement le compte marqué `estAssoue = true`.
3. S'il existe : le signalement est **directement** rattaché à ce compte
   (statut `PRIS_EN_CHARGE` dès la création, pas de passage par
   `EN_ATTENTE`) et un SMS de confirmation lui est envoyé.
4. S'il n'existe pas encore (aucun compte As'Soué créé) : le signalement
   reste `EN_ATTENTE`, un avertissement est simplement noté côté serveur —
   rien ne bloque, mais personne n'est prévenu tant que ce compte n'existe
   pas.

**Cas 2 — Tous les autres types (`PLASTIQUE`, `AUTRE`) : marketplace concurrentiel**
1. Le signalement est créé au statut `EN_ATTENTE`.
2. **Toutes** les entreprises qui gèrent ce type de déchet sont informées
   par SMS (plus de limite de distance comme avant) — mais **les plus
   proches de la zone du signalement sont contactées en premier** (envoi
   trié par distance croissante).
3. Le signalement reste ensuite visible dans le fil marketplace de chaque
   entreprise concernée, trié du plus proche au plus loin, jusqu'à ce que
   l'une d'elles le prenne en charge (section 8).

**Ce qui doit te sembler logique :** un signalement PNEU **ne passe jamais**
par le fil marketplace des entreprises — il est directement et
automatiquement affecté à As'Soué. Les autres types restent un système
"premier arrivé, premier servi" entre plusieurs entreprises partenaires.

**Sur les photos :** la compression à 800px et le stockage sont maintenant
réellement implémentés côté serveur (section 11 plus bas) — ce n'est plus
une simple URL fournie de l'extérieur.

---

## 7. Suivre ses signalements

`GET /signalements/mes-signalements` — historique de l'utilisateur
connecté (citoyen ou entreprise), trié du plus récent au plus ancien.

---

## 8. Le fil marketplace (ENTREPRISE)

1. `GET /signalements/marketplace` — réservé au rôle ENTREPRISE. Ne
   contient **jamais** de signalements PNEU (déjà rattachés directement à
   As'Soué, voir section 6).
2. `PATCH /signalements/{id}/prendre-en-charge` — première entreprise qui
   clique le récupère (statut `PRIS_EN_CHARGE`). Une autre qui essaie
   ensuite reçoit un 409.
3. `PATCH /signalements/{id}/collecter` — uniquement par l'entreprise qui
   l'a pris en charge (403 sinon), passe au statut `COLLECTE`.

---

## 9. Vue admin des signalements

`GET /signalements/admin` (tous statuts), `PATCH
/signalements/admin/{id}/statut` (changement manuel, cas exceptionnels),
`GET /signalements/export` (CSV). Tout réservé à l'ADMIN.

---

## 10. Formations pour la population (nouveau)

As'Soué peut proposer des formations (tri des déchets, recyclage,
entrepreneuriat vert...) auxquelles n'importe quel utilisateur connecté
peut s'inscrire.

1. `POST /formations` — **ADMIN uniquement** : titre, description, lieu,
   date (doit être dans le futur), et un nombre de places optionnel (vide =
   illimité).
2. `GET /formations` et `GET /formations/{id}` — **publics**, consultables
   sans être connecté, comme le catalogue de produits.
3. `POST /formations/{id}/inscription` — **connecté, tous rôles** (CITOYEN,
   ENTREPRISE, ADMIN). Deux garde-fous :
   - impossible de s'inscrire deux fois à la même formation (409 sinon) ;
   - si un nombre de places est fixé et déjà atteint, la formation est
     "complète" (409).
4. Une inscription réussie envoie un SMS de confirmation (titre, date,
   lieu).
5. `GET /formations/mes-inscriptions` — historique de l'utilisateur
   connecté.
6. `GET /formations/{id}/inscrits` — **ADMIN uniquement** : qui s'est
   inscrit (nom, téléphone) pour préparer la logistique.

---

## 11. Photos : compression et stockage (nouveau)

**Ta question de départ était la bonne : non, les photos ne sont pas
stockées dans PostgreSQL** — ce serait effectivement une mauvaise idée
(base qui grossit vite, sauvegardes lentes et coûteuses, PostgreSQL n'est
pas fait pour héberger des fichiers binaires volumineux).

**Ce qui est implémenté maintenant :**
1. `POST /signalements/photos` (upload du fichier image, avant de créer le
   signalement) :
   - vérifie que c'est bien une image (jpeg/png/webp), taille max 10 Mo
     avant compression ;
   - la redimensionne automatiquement pour que sa plus grande dimension ne
     dépasse jamais 800px (proportions conservées) ;
   - l'enregistre **sur le disque du serveur** (pas en base de données),
     dans un dossier configurable (`photos.storage-path`) ;
   - renvoie une URL (`photoUrl`) à réutiliser tel quel dans
     `POST /signalements`.
2. Ces photos sont servies directement en statique par le serveur sous
   `/photos-signalements/...` — accessible sans authentification (comme
   n'importe quelle image sur le web, une balise `<img>` ne peut pas
   envoyer de jeton JWT).
3. En Docker, ce dossier est un **volume persistant** : les photos
   survivent aux redémarrages du conteneur (`docker compose down` sans
   `-v` ne les efface pas).

**Alternatives si le volume de photos devient important :**
- **MinIO** (auto-hébergé, compatible S3, gratuit) : bon compromis si tu
  veux garder les données au Burkina Faso/chez toi sans dépendre d'un
  service étranger. Demande un conteneur Docker supplémentaire.
- **Un vrai fournisseur S3-compatible** (AWS S3, Scaleway, OVH Object
  Storage, Backblaze B2) : zéro maintenance de serveur, facturation à
  l'usage, mais dépendance à un fournisseur externe.
- **Cloudinary ou équivalent** : gère la compression/redimensionnement à
  la volée en plus du stockage, pratique mais gratuit seulement jusqu'à un
  certain volume.

Le code actuel (stockage disque) fonctionne très bien pour démarrer et ne
coûte rien de plus que le serveur lui-même. Le jour où ça devient un vrai
sujet (plusieurs Go de photos, besoin de répliquer sur plusieurs
serveurs), il suffira de remplacer le contenu de `PhotoStorageService`
par un client MinIO/S3 — le reste de l'application ne verra aucune
différence, elle continue de recevoir une URL en retour.

---

## 12. Passer une commande et payer (vente de produits recyclés)

Résumé rapide (détaillé dans le workflow complet plus bas) :
1. `POST /commandes` : le serveur recalcule lui-même prix et disponibilité
   de chaque produit (jamais les valeurs envoyées par le client).
2. Une transaction de paiement est initiée automatiquement en interne.
3. `POST /paiements/webhook` (signature HMAC obligatoire) confirme ou non
   le paiement — c'est le seul point d'entrée externe pour Orange
   Money/Moov Money, encore simulé en attendant les vraies clés API.
4. La commande change de statut automatiquement, un SMS récapitulatif est
   envoyé si le paiement est confirmé.

---

## 13. Notifications SMS

Aucune route HTTP publique pour "envoyer un SMS" — toujours un appel
interne (`SmsGatewayService`). `GET /notifications` (ADMIN) donne
l'historique complet (utile pour "le client dit ne pas avoir reçu son
code"). L'envoi est **simulé** (juste enregistré en base comme réussi) —
à remplacer par un vrai fournisseur SMS avant la mise en production.

---

## 14. Intégration OpenStreetMap (Nominatim) — les deux sens

L'application utilise l'API **Nominatim** d'OpenStreetMap (gratuite, sans
clé), dans les deux sens demandés :

**1. Coordonnées → adresse (reverse geocoding)**
- Utilisé automatiquement à chaque création de signalement (section 6) et
  d'entreprise (section 5) quand des coordonnées GPS sont fournies : le
  backend appelle Nominatim en interne pour obtenir une adresse lisible,
  stockée à côté des coordonnées brutes.
- Toujours **best-effort** : si Nominatim ne répond pas, l'opération
  continue normalement, juste sans adresse (`adresse: null`).

**2. Adresse → coordonnées (forward geocoding)**
- Utilisé quand l'ADMIN crée une entreprise en tapant une adresse plutôt
  qu'en fournissant des coordonnées GPS directement.
- Contrairement au reverse, celui-ci **doit** réussir pour continuer : si
  Nominatim ne trouve rien pour l'adresse donnée, la création échoue avec
  une erreur claire (400) plutôt que de créer une entreprise sans
  localisation exploitable (indispensable pour le calcul de distance du
  matching marketplace, section 6).

**Deux routes exposées directement au frontend** (utile pour une recherche
d'adresse interactive, ex. une boîte de saisie avec suggestions) :
- `GET /geocodage/adresse?latitude=...&longitude=...` → `{ adresse }`
- `GET /geocodage/coordonnees?adresse=...` → `{ latitude, longitude,
  adresseTrouvee }`

Ces deux routes nécessitent d'être connecté (comme la plupart des routes
de l'API) — volontaire, pour éviter que n'importe qui sur Internet
utilise ton backend comme relais anonyme vers l'API gratuite d'OpenStreetMap.

**Limite technique importante :** l'instance publique de Nominatim impose
au maximum **1 requête par seconde**, tous appels confondus (politique
officielle d'OpenStreetMap). Le backend respecte cette limite lui-même
(il ralentit ses propres appels si besoin) — mais si le volume de
signalements/entreprises créés devient important, ou si plusieurs
instances du backend tournent en parallèle derrière un répartiteur de
charge, cette limite ne suffira plus. Dans ce cas, il faudra basculer
vers un fournisseur payant compatible (LocationIQ, Geoapify, Mapbox) —
aucun changement de code n'est nécessaire au-delà de l'URL de base
configurée (`geocoding.nominatim-url`), tant que le format de réponse
reste compatible Nominatim.

---

## 15. Sécurité — rappel

- JWT obligatoire sur toute route qui n'est pas explicitement publique.
- Mot de passe jamais stocké en clair (bcrypt).
- Nom et téléphone chiffrés en base (AES-256-GCM) ; recherche par
  téléphone via un hachage séparé, jamais par comparaison en clair.
- 2FA SMS obligatoire pour tout compte ADMIN.

---

# Workflows complets, de bout en bout

Les sections précédentes décrivent chaque brique séparément. Voici deux
scénarios complets, du début à la fin, pour voir comment tout s'enchaîne
dans un cas réel.

## Workflow A — Un citoyen signale des déchets, une entreprise vient les collecter

```
CITOYEN                    SERVEUR                         ENTREPRISE(S)
   |                          |                                  |
   | 1. Ouvre l'app,          |                                  |
   |    prend une photo       |                                  |
   |    du dépôt sauvage      |                                  |
   |------------------------->|                                  |
   |  POST /signalements/     |                                  |
   |  photos (fichier image)  |                                  |
   |                          | 2. Compresse la photo à 800px    |
   |                          |    max, la sauvegarde sur disque |
   |<-------------------------|                                  |
   |  { photoUrl: "/photos-   |                                  |
   |  signalements/xxx.jpg" } |                                  |
   |                          |                                  |
   | 3. Le téléphone capture  |                                  |
   |    sa position GPS       |                                  |
   |------------------------->|                                  |
   |  POST /signalements      |                                  |
   |  { typeDechet, photoUrl, |                                  |
   |    latitude, longitude } |                                  |
   |                          | 4. Enregistre le signalement     |
   |                          |    (statut EN_ATTENTE), génère   |
   |                          |    un numéro de suivi SIG-xxxxxx |
   |                          |                                  |
   |                          |    Type PNEU ?                   |
   |                          |    +-- OUI : rattache direct-    |
   |                          |    |   ement au compte As'Soué,  |
   |                          |    |   statut PRIS_EN_CHARGE     |
   |                          |    |   immédiatement ------------|-> SMS à As'Soué
   |                          |    |                              |
   |                          |    +-- NON (PLASTIQUE/AUTRE) :   |
   |                          |        cherche toutes les        |
   |                          |        entreprises de ce type,   |
   |                          |        trie par distance ---------|-> SMS aux
   |<-------------------------|                                  |   entreprises,
   |  201 Created             |                                  |   la plus proche
   |  { numeroSuivi, statut:  |                                  |   en premier
   |    EN_ATTENTE }          |                                  |
   |                          |                                  |
   |                          |                    5. Une entreprise ouvre
   |                          |                       son fil marketplace
   |                          |<---------------------------------|
   |                          |  GET /signalements/marketplace   |
   |                          |------------------ (trié par distance) ->|
   |                          |                                  |
   |                          |                    6. Elle clique
   |                          |                       "prendre en charge"
   |                          |<---------------------------------|
   |                          |  PATCH /signalements/{id}/       |
   |                          |  prendre-en-charge               |
   |                          |                                  |
   |                          | 7. Statut -> PRIS_EN_CHARGE,      |
   |                          |    entrepriseId fixé.            |
   |                          |    Si une 2e entreprise tente     |
   |                          |    la même action -> 409 refusé   |
   |                          |                                  |
   |                          |                    8. L'entreprise se
   |                          |                       déplace, collecte
   |                          |                       physiquement
   |                          |<---------------------------------|
   |                          |  PATCH /signalements/{id}/       |
   |                          |  collecter                       |
   |                          |                                  |
   |                          | 9. Verifie que c'est bien elle    |
   |                          |    qui l'avait pris en charge     |
   |                          |    (403 sinon), statut -> COLLECTE|
   |                          |                                  |
   | 10. GET /signalements/   |                                  |
   |     mes-signalements     |                                  |
   |------------------------->|                                  |
   |<-------------------------|                                  |
   |  Voit son signalement    |                                  |
   |  au statut COLLECTE      |                                  |
```

**Points clés de ce workflow :**
- Le citoyen n'est jamais notifié activement quand c'est collecté (pas de
  SMS de fin) — il doit consulter `mes-signalements` pour voir l'évolution.
  Si tu veux un SMS "votre signalement a été collecté", c'est à ajouter
  dans `SignalementService.marquerCollecte()`.
- Pour PNEU, les étapes 5 à 9 (marketplace, concurrence entre entreprises)
  n'existent pas : tout se passe entre les étapes 4 et 10 directement.
- Si aucune entreprise du bon type n'existe en base (aucune n'a été créée
  par l'ADMIN pour ce type de déchet), le signalement reste éternellement
  `EN_ATTENTE` — personne n'est notifié, mais rien ne plante non plus.

---

## Workflow B — Un citoyen achète un produit recyclé (vente d'objet)

```
CITOYEN                    SERVEUR                    OPÉRATEUR MOBILE MONEY
   |                          |                                  |
   | 1. Parcourt le           |                                  |
   |    catalogue (public,    |                                  |
   |    pas besoin de compte) |                                  |
   |------------------------->|                                  |
   |  GET /produits           |                                  |
   |<-------------------------|                                  |
   |  Liste des produits      |                                  |
   |  disponibles             |                                  |
   |                          |                                  |
   | 2. Se connecte           |                                  |
   |    (ou crée un compte    |                                  |
   |    s'il n'en a pas)      |                                  |
   |------------------------->|                                  |
   |  POST /auth/connexion    |                                  |
   |<-------------------------|                                  |
   |  { token: "xxx" }        |                                  |
   |                          |                                  |
   | 3. Ajoute des articles   |                                  |
   |    au panier, choisit    |                                  |
   |    Orange Money          |                                  |
   |------------------------->|                                  |
   |  POST /commandes         |                                  |
   |  { lignes: [...],        |                                  |
   |    operateur: ORANGE }   |                                  |
   |                          | 4. Pour CHAQUE ligne, relit le   |
   |                          |    prix et la disponibilité      |
   |                          |    réels en base (jamais ce que  |
   |                          |    le client a envoyé)           |
   |                          |                                  |
   |                          |    Produit indisponible ?         |
   |                          |    +-- OUI : 409, rien n'est      |
   |                          |        enregistré, on s'arrête   |
   |                          |        ici                        |
   |                          |                                  |
   |                          | 5. Calcule le total, crée la      |
   |                          |    commande (statut               |
   |                          |    EN_ATTENTE_PAIEMENT)          |
   |                          |                                  |
   |                          | 6. Initie automatiquement une     |
   |                          |    transaction de paiement        |
   |                          |    (statut INITIEE, référence     |
   |                          |    SIM-xxxxxxxx tant que la vraie |
   |                          |    intégration n'est pas          |
   |                          |    branchée)                      |
   |<-------------------------|                                  |
   |  201 Created              |                                  |
   |  { commande, statut:      |                                  |
   |    EN_ATTENTE_PAIEMENT }  |                                  |
   |                          |                                  |
   | 7. Reçoit une demande     |                                  |
   |    de code sur son        |                                  |
   |    téléphone (côté        |                                  |
   |    opérateur, hors        |                                  |
   |    de cette API)          |                                  |
   |                          |                                  |
   |                          |          8. L'opérateur confirme le
   |                          |             paiement en rappelant
   |                          |<---------------------------------|
   |                          |  POST /paiements/webhook          |
   |                          |  + en-tête X-Webhook-Signature    |
   |                          |                                  |
   |                          | 9. Vérifie la signature HMAC AVANT
   |                          |    de lire quoi que ce soit       |
   |                          |    (signature absente/fausse      |
   |                          |    -> 401, rien n'est traité)     |
   |                          |                                  |
   |                          | 10. Marque la transaction         |
   |                          |     CONFIRMEE, publie un          |
   |                          |     évènement interne             |
   |                          |     (PaiementConfirmeEvent)       |
   |                          |                                  |
   |                          | 11. La commande passe             |
   |                          |     automatiquement à CONFIRMEE   |
   |                          |     (écoute de l'évènement,       |
   |                          |     aucun appel HTTP en plus)     |
   |<-------------------------|                                  |
   |  SMS : "Commande #12      |                                  |
   |  confirmée, montant       |                                  |
   |  6000 FCFA"               |                                  |
   |                          |                                  |
   | 12. Consulte son          |                                  |
   |     historique            |                                  |
   |------------------------->|                                  |
   |  GET /commandes/          |                                  |
   |  mes-commandes            |                                  |
   |<-------------------------|                                  |
   |  Voit sa commande         |                                  |
   |  au statut CONFIRMEE      |                                  |
   |                          |                                  |
   |                          |          ... plus tard, logistique
   |                          |<---------------------------------|
   |                          |  ADMIN : PATCH /commandes/admin/  |
   |                          |  {id}/statut -> EN_PREPARATION    |
   |                          |  puis LIVREE (manuel, aucune      |
   |                          |  automatisation)                  |
```

**Points clés de ce workflow :**
- Tant que les clés Orange/Moov Money ne sont pas fournies par As'Soué,
  **rien dans l'interface utilisateur ne peut déclencher l'étape 8** — il
  faut la simuler manuellement (avec une signature HMAC valide) pour
  tester le parcours complet en attendant l'intégration réelle.
- Si le paiement échoue plutôt que réussir (`succes: false` dans le
  webhook), la commande passe à `ECHOUEE` au lieu de `CONFIRMEE`, et
  **aucun SMS n'est envoyé** — le citoyen doit consulter son historique
  pour s'en rendre compte. Si tu veux un SMS d'échec aussi, c'est à
  ajouter dans `CommandeService.notifierResultatPaiement()`.
- `EN_PREPARATION` et `LIVREE` ne sont jamais mis à jour automatiquement
  par le système : c'est un suivi logistique humain, géré par l'ADMIN au
  fur et à mesure.
