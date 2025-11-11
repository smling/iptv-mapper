FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for healthchecks and agent download
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# make matching group+user with explicit ids
RUN groupadd --gid 10001 appuser \
 && useradd  --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin appuser \
 && chown -R appuser:appuser /app

# Add entrypoint script to handle OTEL agent fetch if needed
COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
RUN ./usr/local/bin/entrypoint.sh

USER appuser

ENV TZ=Europe/London \
    SPRING_PROFILES_ACTIVE=prod \
    APP_INGEST_CRON="0 0 3 * * *" \
    APP_INGEST_ENABLED="true" \
    SPRING_FLYWAY_LOCATIONS=filesystem:/app/db/init \
    JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70 \
               -XX:InitialRAMPercentage=50 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/oom.hprof \
               -Djava.security.egd=file:/dev/urandom \
               "
COPY build/libs/iptv-mapper-*-SNAPSHOT.jar /app/app.jar
COPY scripts/sql/initial /app/db/init
EXPOSE 8080 8443
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
