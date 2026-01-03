<p align="center">
  <img src="./src/main/resources/static/icons/package.svg" alt="FeedBox Logo" width="100"/>
</p>

<h1 align="center">FeedBox</h1>
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

# What is FeedBox?

FeedBox is a simple, self-hosted feed reader built with Java.  
It allows you to manage all your feeds in one place—without ads, tracking, AI, or other distractions.

# Features

Feature highlights include:

- Fully self-hostable
- Automatic dark mode
- Mobile and desktop views
- Import and export feeds as OPML
- Organize feeds into folders
- Periodic background refresh

# Installation

It is possible to run FeedBox via Docker or deploy it to a Kubernetes cluster using the provided Helm chart. In both cases, a PostgreSQL database is required.
The following environment variables need to be set for database connectivity:

| Variable      | Description       |
| ------------- | ----------------- |
| `PG_USER`     | Database user     |
| `PG_PASSWORD` | Database password |
| `PG_HOST`     | Database host     |
| `PG_PORT`     | Database port     |
| `PG_DB`       | Database name     |

Additionally, the application port can be configured using the `PORT` variable (default is 7070).

## Docker

To run FeedBox using Docker, first start a PostgreSQL container, then run the FeedBox container. For testing purposes, you can use the following commands - for production, make sure to follow best practices regarding security and data persistence.

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

## Helm

For installing via Helm, use the following command:

```bash
helm install feedbox oci://ghcr.io/felixzmn/helm/feedbox --version <VERSION>
```

Further configuration options can be found having a look at the `values.yaml` file in the `chart` directory.

# Local Development

## Prerequisites

- PostgreSQL (e.g. via Docker)
- Java
- Maven

## Commit scopes

| Scope     | Description           |
| --------- | --------------------- |
| `backend` | Java-code             |
| `ui`      | Frontend related code |
| `chart`   | Helm chart            |
| `deps`    | Dependencies          |
| `docs`    | Documentation         |

## Build the application

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

## Build and push the Docker image:

```bash
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

docker build -t ghcr.io/felixzmn/docker/feedbox:$VERSION -t ghcr.io/felixzmn/docker/feedbox:latest .
docker push ghcr.io/felixzmn/docker/feedbox:$VERSION
docker push ghcr.io/felixzmn/docker/feedbox:latest
```

## Build and push the Helm chart

```bash
cd chart
helm package .
helm push feedbox-*.tgz oci://ghcr.io/felixzmn/helm
```

# Icons

[Icons from Tabler Icons.](https://tablericons.com/)\
[PWA Icon generated with maskable](https://maskable.app/editor)
