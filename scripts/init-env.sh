#!/usr/bin/env bash
# Genere un fichier .env avec de vrais secrets aleatoires a partir de .env.example.
# Usage : ./scripts/init-env.sh        (Linux, macOS, WSL ou Git Bash sous Windows)
# Ne remplace jamais un .env existant.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo ".env existe deja : rien a faire (supprimez-le d'abord pour le regenerer)."
  exit 0
fi
command -v openssl >/dev/null 2>&1 || { echo "openssl est requis pour generer les secrets."; exit 1; }

alea() { openssl rand -base64 48 | tr -d '\n=+/' | cut -c1-"${1:-48}"; }

sed \
  -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(alea 24)|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(alea 48)|" \
  -e "s|^DATA_ENCRYPTION_KEY=.*|DATA_ENCRYPTION_KEY=$(alea 48)|" \
  -e "s|^PAYMENT_WEBHOOK_SECRET=.*|PAYMENT_WEBHOOK_SECRET=$(alea 48)|" \
  -e "s|^ADMIN_DEFAULT_PASSWORD=.*|ADMIN_DEFAULT_PASSWORD=$(alea 20)|" \
  .env.example > .env
chmod 600 .env

echo ".env cree avec des secrets aleatoires."
echo "IMPORTANT : sauvegardez DATA_ENCRYPTION_KEY (la perdre rend noms et telephones illisibles)."
echo "Mot de passe du compte admin (admin@cif.bf) : voir ADMIN_DEFAULT_PASSWORD dans .env"
