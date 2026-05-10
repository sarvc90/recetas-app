# Single-stage runtime image. JAR is built outside (CI runner or local mvn) and
# placed at docker/backend/app.jar before `docker compose build`.
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tini && \
    addgroup -S recetas && adduser -S recetas -G recetas

WORKDIR /opt/recetas

COPY --chown=recetas:recetas app.jar /opt/recetas/app.jar

USER recetas
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/sbin/tini","--","sh","-c","exec java $JAVA_OPTS -jar /opt/recetas/app.jar"]
