FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tini && \
    addgroup -S recetas && adduser -S recetas -G recetas

WORKDIR /opt/recetas

COPY --chown=recetas:recetas backend/recetas.jar /opt/recetas/recetas.jar

# FIJACIÓN DE SEGURIDAD: Quitamos permisos de escritura (chmod 555)
# Esto asegura que el archivo sea Read-Only para todos, cumpliendo con SonarCloud.
RUN chmod 555 /opt/recetas/recetas.jar

USER recetas
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/sbin/tini","--","sh","-c","exec java $JAVA_OPTS -jar /opt/recetas/recetas.jar"]
