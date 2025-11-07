FROM eclipse-temurin:25-jre
WORKDIR /app

# make matching group+user with explicit ids
RUN groupadd --gid 10001 appuser \
 && useradd  --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin appuser \
 && chown -R appuser:appuser /app

USER appuser

ENV TZ=Europe/London SPRING_PROFILES_ACTIVE=prod APP_INGEST_CRON="0 0 3 * * *" APP_INGEST_ENABLED="true" JAVA_OPTS="" SPRING_FLYWAY_LOCATIONS=filesystem:/app/db/init
COPY build/libs/iptv-mapper-*-SNAPSHOT.jar /app/app.jar
COPY scripts/sql/initial /app/db/init
EXPOSE 8080 8443
ENTRYPOINT ["sh","-lc","exec java $JAVA_OPTS -Duser.timezone=$TZ -jar /app/app.jar"]
