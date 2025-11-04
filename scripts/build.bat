# Build the image
docker build -t iptv-mapper:latest .

# Run (example)
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/iptv-mapper" \
  -e APP_INGEST_CRON="0 0 3 * * *" \
  -e APP_INGEST_ENABLED="true" \
  iptv-mapper:latest
