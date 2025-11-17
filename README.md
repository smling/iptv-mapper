# 📺 IPTV Mapper

**IPTV Mapper** is a Java 21 / Spring Boot application for handling IPTV playlists (`M3U`) and Electronic Program Guide (EPG) data. It exposes RESTful APIs for managing data sources, serving playlists, and accessing EPG metadata.

## ✨ Features

- **Data source management** – CRUD APIs for IPTV data sources.
- **M3U playlist handling** – Generate and serve M3U playlists from stored channels.
- **EPG handling** – Ingest and query EPG data (programmes, schedules).
- **Automated ingestion** – Scheduled ingestion of M3U + EPG sources and cleanup of old programme data.
- **OpenAPI documentation** – Interactive API docs via Springdoc (Swagger UI).

## 📦 Installation

### Prerequisites

Ensure the following are installed:

| Tool                 | Link                                      |
|----------------------|-------------------------------------------|
| ☕ **Java JDK 21**    | [Download JDK](https://adoptium.net/)     |
| 🐳 **Docker**        | [Get Docker](https://docs.docker.com/get-docker/) |
| 🧩 **Docker Compose**| [Install Docker Compose](https://docs.docker.com/compose/install/) |
| 📦 **Gradle (optional)** | Not required if you use the included `gradlew` wrapper |

### Using Gradle (local run)

To compile and run the application locally:

1. **Navigate** to the project directory:
   ```shell
   cd iptv-mapper
   ```

2. **Build the project**:
   ```shell
   ./gradlew clean build
   ```

3. **Run the application**:
   ```shell
   ./gradlew bootRun
   ```

By default the app listens on port `8080` (configurable via `SERVER_PORT`).

### Using Docker

#### 🏗️ Build the Docker image

From the project root:

```shell
docker build -t iptv-mapper:latest .
```

#### 🚀 Start the application with Docker Compose

1. Ensure `docker-compose.yml` is in the project root.
2. Start the stack:

   ```shell
   docker compose up -d
   ```

- **Services started**:  
  - 🗄️ **PostgreSQL** (`postgres:16`)
  - 📺 **IPTV Mapper application**

#### 🌐 Access the application

- **HTTP API / UI**: `http://localhost:8080`
- **HTTPS** (if configured): `https://localhost:8443`
- **OpenAPI / Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

#### 🧰 Manage the services

- **View logs**:
  ```shell
  docker compose logs -f
  ```

- **Stop services**:
  ```shell
  docker compose down
  ```

## ⚙️ Environment configuration

Customize the application by updating `docker-compose.yml` or using environment variables locally. Key settings:

| Variable                 | Description                                              | Default                                         |
|--------------------------|----------------------------------------------------------|-------------------------------------------------|
| `APP_INGEST_ENABLED`     | Enable/disable scheduled ingestion                       | `true`                                          |
| `APP_INGEST_CRON`        | Cron expression for ingestion schedule (UTC)             | `0 0 3 * * *`                                   |
| `APP_CLEANUP_DAYS`       | Days of programme data to retain before cleanup          | `7`                                             |
| `DB_URL`                 | JDBC URL for the primary database                        | `jdbc:postgresql://localhost:5432/iptv-mapper` |
| `DB_USER`                | Database username                                        | `iptv-mapper`                                   |
| `DB_PASSWORD`            | Database password                                        | `change-me`                                     |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`, `prod`, etc.)              | `prod`                                          |
| `SERVER_PORT`            | HTTP server port                                         | `8080`                                          |
| `IPTV_BASE_URL`          | Base URL used when generating playlist links             | `http://localhost:8080`                         |

Flyway, JPA, and OpenTelemetry settings can also be tuned via environment variables; see `application.yml` and `docker-compose.yml` for the full list.

## ✅ Testing

Run the test suite with:

```shell
./gradlew test
```

To generate a coverage report:

```shell
./gradlew test jacocoTestReport
```

## 🤝 Contributing

Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request with a clear description of your changes and any relevant tests.

## 📄 License

This project is licensed under the MIT License.
