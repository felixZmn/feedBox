<p align="center">
  <img src="./src/main/resources/META-INF/resources/icons/package.svg" alt="FeedBox Logo" width="100"/>
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

FeedBox uses Quarkus configuration directly. Set the JDBC URL in the environment (or via config files):

- `QUARKUS_DATASOURCE_JDBC_URL`: e.g. `jdbc:postgresql://<host>:<port>/<db>?user=<user>&password=<password>`

Optional overrides:

- `QUARKUS_HTTP_PORT`: HTTP port (default `8080`)
- `APP_HTTP_USER_AGENT` / `app.http.user-agent` in `src/main/resources/application.properties`

If you use the Helm chart, the default service port is `8080`.

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
  -p 8080:8080 \
  --network appnet \
  ghcr.io/felixzmn/docker/feedbox:latest
```

## Helm

For installing via Helm, use the following command:

```bash
helm install feedbox oci://ghcr.io/felixzmn/helm/feedbox --version <VERSION>
```

Default configuration values can be found in `values.yaml` in the Helm chart. You can override these values by providing a custom `values.yaml` file or using `--set` flags.

## Configuration Parameters

The following environment variables / properties can be configured:

| Parameter                          | Default         | Description                          |
| ---------------------------------- | --------------- | ------------------------------------ |
| `QUARKUS_DATASOURCE_JDBC_URL`      | -               | ! Database connection URL (required) |
| `QUARKUS_DATASOURCE_JDBC_MAX_SIZE` | `16`            | Maximum connection pool size         |
| `MP_JWT_VERIFY_PUBLICKEY_LOCATION` | -               | JWT public key URL                   |
| `MP_JWT_VERIFY_ISSUER`             | -               | JWT issuer validation                |
| `SSO_CLIENTID`                     | -               | SSO client ID                        |
| `SSO_REDIRECTURI`                  | -               | SSO redirect URI after login         |
| `SSO_AUTHSERVERURL`                | -               | SSO authorization server URL         |
| `SSO_ENDSESSIONENDPOINT`           | -               | SSO logout endpoint                  |
| `QUARKUS_HTTP_PORT`                | `8080`          | HTTP port for the application        |
| `APP_HTTP_USER_AGENT`              | `FeedBox/2.0.5` | User agent string for HTTP requests  |
| `REFRESH_JOB_INTERVAL`             | `1h`            | Background feed refresh interval     |
| `APP_HTTP_TIMEOUT`                 | `30`            | HTTP request timeout in seconds      |

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
| `deps`    | Dependencies          |
| `docs`    | Documentation         |

## Build the application

```bash
mvn install -Dnative

```

Run the application:

```bash
java -jar target/*-runner.jar
```

## Build and push the Docker image:

```bash
mvn install -Dnative
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')
docker build -f src/main/docker/Dockerfile.native-micro -t ghcr.io/felixzmn/docker/feedbox:latest -t ghcr.io/felixzmn/docker/feedbox:$VERSION .
docker push ghcr.io/felixzmn/docker/feedbox:$VERSION
docker push ghcr.io/felixzmn/docker/feedbox:latest
```

# Release Process

The release process is documented in [release.md](./release.md).

# Icons

[Icons from Tabler Icons.](https://tablericons.com/)\
[PWA Icon generated with maskable](https://maskable.app/editor)
