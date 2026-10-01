# ---- Etape 1 : build Angular ----
FROM node:22-alpine AS build
WORKDIR /app

# package.json + package-lock.json d'abord : tant qu'ils ne changent pas,
# Docker reutilise le cache de "npm ci" (la partie la plus longue du build).
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration production

# ---- Etape 2 : service des fichiers statiques par Nginx ----
FROM nginx:1.27-alpine

# Angular 17+ (builder @angular/build:application) sort les fichiers du
# navigateur dans dist/<projet>/browser -- d'ou le /browser ci-dessous.
COPY --from=build /app/dist/assoue/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost/ >/dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
