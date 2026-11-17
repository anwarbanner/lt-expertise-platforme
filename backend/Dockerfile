# Backend LT Expertise : construit avec Maven, exécuté sur un JRE 21 sous un utilisateur sans privilèges.
FROM maven:3.9-eclipse-temurin-21 AS construction
WORKDIR /construction
# Dépendances d'abord : la couche reste en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
# Les tests tournent hors de l'image (Testcontainers a besoin de Docker) : mvn test avant de construire
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
# fontconfig : rendu des polices du rapport PDF (JasperReports) ; curl : contrôle de santé
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app lt
WORKDIR /app
COPY --from=construction /construction/target/lt-expertise-backend-*.jar app.jar
USER lt
ENV TZ=Africa/Casablanca \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Africa/Casablanca"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
