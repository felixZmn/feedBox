<p align="center">
  <img src="./src/main/resources/static/icons/package.svg" alt="FeedBox Logo" width="100"/>
</p>

<h1 align="center">feedBox</h1>
<h3 align="center">Put all your feeds into a box!</h3>

<p align="center">
  <!-- Build / CI -->
  <!-- <a href="https://github.com/felixzmn/feedbox/actions">
    <img src="https://github.com/felixzmn/feedbox/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a> -->
  <!-- Latest Release -->
  <a href="https://github.com/felixzmn/feedbox/releases">
    <img src="https://img.shields.io/github/v/release/felixzmn/feedbox?logo=github" alt="Latest Release"/>
  </a>
  <!-- Docker Image -->
  <a href="https://ghcr.io/felixzmn/docker/feedbox">
    <img src="https://img.shields.io/badge/docker-ghcr.io%2Ffeedbox-blue?logo=docker" alt="Docker Image"/>
  </a>
  <!-- Helm Chart -->
  <a href="https://ghcr.io/felixzmn/helm/feedbox">
    <img src="https://img.shields.io/badge/helm-chart-blue?logo=helm" alt="Helm Chart"/>
  </a>
  <!-- License -->
  <a href="./LICENSE">
    <img src="https://img.shields.io/github/license/felixzmn/feedbox" alt="License"/>
  </a>
</p>

![Desktop Light Mode](./docs/images/desktop-light.png)

# What is feedBox?

FeedBox is a simple, self-hosted feed reader built with Java.  
It allows you to manage all your feeds in one place—without ads, tracking, AI, or other distractions.

# Features

⚠️ **Currently in non-production state**. Planned features include:

- Simple UI with automatic dark mode
- Mobile and desktop views
- Fully self-hostable
- Import and export feeds as OPML
- Organize feeds into folders
- Periodic background refresh

# Getting Started

## Local Development

**Prerequisites**:

- PostgreSQL (e.g. via Docker)
- Java
- Maven

**Build the application**:

```bash
mvn clean install
```

Run the application:

```bash
PG_USER=user \
PG_PASSWORD=password \
PG_HOST=127.0.0.1 \
PG_PORT=5432 \
PG_DB=postgres \
java -jar target/feedBox.jar
```

Build and push Docker image:

```bash
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

docker build -t ghcr.io/felixzmn/docker/feedbox:$VERSION .
docker push ghcr.io/felixzmn/docker/feedbox:$VERSION
```

Running with Docker

```bash
docker network create appnet

docker run -d --name postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=user \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=postgres \
  --network appnet \
  postgres

docker run --rm --name feedbox \
  -p 7070:7070 \
  --network appnet \
  ghcr.io/felixzmn/docker/feedbox:latest
```

## Helm Chart

### Build the chart

```bash
cd chart
helm package .
helm push feedbox-*.tgz oci://ghcr.io/felixzmn/helm
```

### Deploy to your cluster

```bash
helm install feedbox oci://ghcr.io/felixzmn/helm/feedbox --version <VERSION>
```

# Configuration

| Variable       | Required | Description           | Default |
| -------------- | -------- | --------------------- | ------- |
| `PG_USER`      | yes      | Database user         |         |
| `PG_PASSWORD`  | yes      | Database password     |         |
| `PG_HOST`      | yes      | Database host         |         |
| `PG_PORT`      | yes      | Database port         |         |
| `PG_DB`        | yes      | Database name         |         |
| `PORT`         | no       | Application port      | 7070    |
| `REFRESH_RATE` | no       | Feed refresh interval | 60min   |

# Icons

[Icons from Tabler Icons.](https://tablericons.com/)
