# ---- Etape 1 : build Maven ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le pom.xml en premier pour profiter du cache Docker sur les dependances
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Etape 2 : image d'execution (legere) ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

# Le stockage des photos de signalement est un volume Docker nomme (voir
# docker-compose.yml : assoue_photos_signalements:/data/photos-signalements).
# Un volume nomme encore vide est initialise par Docker avec le contenu ET
# les permissions du dossier de l'IMAGE a ce chemin -- d'ou la creation +
# chown ICI, avant USER spring. Sans ca, le point de montage est cree a la
# volee par le moteur Docker et appartient a root, et l'appli (qui tourne en
# "spring", non-root) plante avec "Permission denied" au premier upload de
# photo.
RUN mkdir -p /data/photos-signalements && chown -R spring:spring /data/photos-signalements

USER spring

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
