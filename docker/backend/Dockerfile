# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY recetas-app/pom.xml .
RUN mvn dependency:go-offline -q

COPY recetas-app/src ./src
RUN mvn package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tini && \
    addgroup -S recetas && adduser -S recetas -G recetas

WORKDIR /opt/recetas

COPY --from=builder --chown=recetas:recetas /build/target/recetas-app-0.0.1-SNAPSHOT.jar /opt/recetas/recetas.jar

USER recetas
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/sbin/tini","--","sh","-c","exec java $JAVA_OPTS -jar /opt/recetas/recetas.jar"]
