# Release Process

Versions follow the pattern `MAJOR.MINOR.PATCH`. During development, `pom.xml` carries a `-SNAPSHOT` suffix.

## 1. Prepare the release

Remove the `-SNAPSHOT` suffix from all version fields:

- **`pom.xml`** – e.g. `1.0.3-SNAPSHOT` → `1.0.3`
- **`src/main/resources/application.properties`** – e.g. `app.http.user-agent=FeedBox/1.0.3-SNAPSHOT` → `app.http.user-agent=FeedBox/1.0.3`

## 2. Commit the release

```bash
git add pom.xml src/main/resources/application.properties
git commit -m "chore: release 2.0.4"
git tag 2.0.4
```

## 3. Build and push the Docker image

```bash
mvn install -Dnative
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

docker build -f src/main/docker/Dockerfile.native-micro -t ghcr.io/felixzmn/docker/feedbox:latest -t ghcr.io/felixzmn/docker/feedbox:$VERSION .
docker push ghcr.io/felixzmn/docker/feedbox:$VERSION
docker push ghcr.io/felixzmn/docker/feedbox:latest
```

## 5. Bump versions back to the next snapshot

After the release artifacts are published, advance to the next development version:

- **`pom.xml`** – increment the patch version and add `-SNAPSHOT`, e.g. `1.0.3` → `1.0.4-SNAPSHOT`

Commit the result:

```bash
git add pom.xml src/main/resources/application.properties
git commit -m "chore: bump version to 2.0.5-SNAPSHOT"
```
