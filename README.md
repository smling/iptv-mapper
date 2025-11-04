# 📺 IPTV Mapper

**IPTV Mapper** is a Java-based application designed for handling IPTV playlists (`M3U`) and Electronic Program Guide (EPG) data. It provides RESTful APIs to facilitate integration and consumption by clients.

## 🌟 Features

- **Data Source Management**: Endpoints for managing IPTV data sources.
- **M3U Playlist Handling**: Generate and serve M3U playlists.
- **EPG Handling**: Retrieve and process EPG data.
- **Ingestion Services**: Automated ingestion of both M3U and EPG data sources.

## ⚙️ Installation

### Prerequisites

Ensure the following are installed:

| Tool              | Link                                      |
|-------------------|-------------------------------------------|
| 📦 **Java JDK 17**| [Download JDK](https://adoptopenjdk.net/) |
| 🚀 **Gradle**     | [Install Gradle](https://gradle.org/install/) |
| 🐳 **Docker**     | [Get Docker](https://docs.docker.com/get-docker/) |
| 🐙 **Docker Compose** | [Install Docker Compose](https://docs.docker.com/compose/install/) |

### Using Gradle

#### 🏗️ Compile and Run Locally

To compile and run the application locally using Gradle:

1. **Navigate** to the project directory:
   ```shell
   cd iptv_mapper
   ```

2. **Build the project** using Gradle:
   ```shell
   ./gradlew build
   ```

3. **Run the application**:
   ```shell
   ./gradlew bootRun
   ```

### Using Docker

#### 🏗️ Build the Docker Image

Navigate to the project's root directory and execute:

```shell
docker build -t iptv-mapper:latest .
```

#### 🚀 Start the Application with Docker Compose

1. Ensure `docker-compose.yml` is in the project root.
2. Run the following command:

   ```shell
   docker-compose up -d
   ```

- **Services Started**:  
  - 🗃️ **Database** with `postgres:16`
  - 🛠️ **IPTV Mapper Application**

#### 🌐 Access the Application

- **HTTP**: [http://localhost:80](http://localhost)
- **HTTPS**: [https://localhost:443](https://localhost)

#### 📋 Manage the Services

- **View Logs**:  
  ```shell
  docker-compose logs -f
  ```

- **Stop Services**:  
  ```shell
  docker-compose down
  ```

## 🌍 Environment Configuration

Customize the application by updating `docker-compose.yml` or using environment variables locally:

| Variable                   | Description                                | Default        |
|----------------------------|--------------------------------------------|----------------|
| `APP_INGEST_ENABLED`       | Enable/disable the ingest process          | `true`         |
| `APP_INGEST_CRON`          | Cron expression for scheduling ingestion   | `0 0 3 * * *`  |
| `SPRING_PROFILES_ACTIVE`   | Set the Spring profile                     | `prod`         |
| `SPRING_DATASOURCE_URL`    | Database URL for connecting                | `jdbc:postgresql://localhost:5432/iptv-mapper` |

## 🔍 Testing

Execute tests locally with Gradle:

```shell
./gradlew test
```

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your improvements or bug fixes.

## 📝 License

This project is licensed under the MIT License.