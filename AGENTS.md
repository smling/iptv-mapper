# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/java`, resources: `src/main/resources`, static assets: `src/main/resources/static`.
- Tests: `src/test/java` (JUnit 5). Keep test packages mirroring source packages.
- Build: Gradle wrapper (`gradlew`, `gradlew.bat`). Java toolchain: 21.
- Database migrations: `scripts/sql/initial/V__*.sql` (Flyway). Add new migration files; do not edit existing ones.
- Docker: `dockerfile`, `docker-compose.yml`. Helper scripts in `scripts/`.

## Build, Test, and Development Commands
- Build all: `./gradlew clean build` — compiles, runs tests, packages.
- Run locally: `./gradlew bootRun` — starts Spring Boot app.
- Tests + coverage: `./gradlew test jacocoTestReport` — JaCoCo HTML at `build/reports/jacoco/test/html/index.html`.
- Docker up: `docker compose up -d` — starts app and Postgres.
- Windows helpers: `scripts\build.bat`, `scripts\deploy.bat`.

## Coding Style & Naming Conventions
- Java 21, 4-space indentation, UTF-8. Keep lines reasonably short (~120 chars).
- Packages: lowercase (`io.github.smling.iptv_mapper...`). Classes: `PascalCase`. Methods/fields: `camelCase`. Constants: `UPPER_SNAKE_CASE`.
- Prefer constructor injection for services/components. Keep controllers thin; push logic to services.
- Files should live under matching package directories; one top-level class per file.

## Testing Guidelines
- Frameworks: JUnit 5; Testcontainers (PostgreSQL) where needed.
- Location/naming: mirror source packages; suffix tests with `*Test.java` (e.g., `EPGClientTest.java`).
- Write focused unit tests; add integration tests for repositories/controllers.
- Ensure `./gradlew test` passes; check coverage report after changes.

## Commit & Pull Request Guidelines
- Commits: concise imperative subject (e.g., "Add EPG parsing for offsets"). Group related changes.
- Prefer Conventional Commit prefixes when practical (`feat:`, `fix:`, `docs:`).
- PRs: include summary, rationale, and screenshots for UI/static changes; link issues.
- Required before merge: tests added/updated, `./gradlew build` green, docs updated (`README.md`, API changes reflected via OpenAPI where relevant).

## Security & Configuration Tips
- App config: `src/main/resources/application.yml`; override via env (e.g., `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`).
- Secrets via environment/compose, not committed files.
- DB changes via new Flyway migrations in `scripts/sql/initial/`.

## Logging Style
- Prefer concise, informative logs with clear context.
- Use emojis to improve readability and scanning, especially for long-running tasks and batch operations. Examples:
  - Start/stop: "📡 Starting …", "✅ Completed …", failures: "❌ …", interruptions: "🛑 …".
  - Summaries: "📊 …", insert/update actions: "➕", "✏️", reuse: "🔁", creation: "🆕".
- Keep sensitive data out of logs (no secrets/tokens). Include IDs and key fields to trace entities.
- Default per-item logs at DEBUG; batch summaries at INFO.
