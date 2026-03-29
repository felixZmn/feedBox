# Release Process

Versions follow the pattern `MAJOR.MINOR.PATCH`. During development, both `pom.xml` and `chart/Chart.yaml` carry a `-SNAPSHOT` suffix. The Helm chart has two separate version fields: `version` (the chart itself) and `appVersion` (the application it ships).

## 1. Prepare the release

Remove the `-SNAPSHOT` suffix from all version fields:

- **`pom.xml`** – e.g. `1.0.3-SNAPSHOT` → `1.0.3`
- **`chart/Chart.yaml`**:
  - `appVersion` – e.g. `"1.0.3-SNAPSHOT"` → `"1.0.3"` (must match `pom.xml`)
  - `version` – e.g. `1.1.6-SNAPSHOT` → `1.1.6` (bump the patch if not already done)
- **`src/main/resources/application.properties`** – e.g. `app.http.user-agent=FeedBox/1.0.3-SNAPSHOT` → `app.http.user-agent=FeedBox/1.0.3`

## 2. Commit the release

```bash
git add pom.xml chart/Chart.yaml src/main/resources/application.properties
git commit -m "chore: release <VERSION>"
```

## 3. Build and push the Docker image

```bash
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

docker build -f src/main/docker/Dockerfile.native-micro -t ghcr.io/felixzmn/docker/feedbox:latest -t ghcr.io/felixzmn/docker/feedbox:$VERSION .
docker push ghcr.io/felixzmn/docker/feedbox:$VERSION
docker push ghcr.io/felixzmn/docker/feedbox:latest
```

## 4. Build and push the Helm chart

```bash
cd chart
helm package .
helm push feedbox-*.tgz oci://ghcr.io/felixzmn/helm
```

## 5. Bump versions back to the next snapshot

After the release artifacts are published, advance to the next development version:

- **`pom.xml`** – increment the patch version and add `-SNAPSHOT`, e.g. `1.0.3` → `1.0.4-SNAPSHOT`
- **`chart/Chart.yaml`**:
  - `appVersion` – match the new `pom.xml` version, e.g. `"1.0.4-SNAPSHOT"`
  - `version` – increment the patch version and add `-SNAPSHOT`, e.g. `1.1.6` → `1.1.7-SNAPSHOT`
- **`src/main/resources/application.properties`** – update the user agent to match the new version, e.g. `app.http.user-agent=FeedBox/1.0.4-SNAPSHOT`

Commit the result:

```bash
git add pom.xml chart/Chart.yaml src/main/resources/application.properties
git commit -m "chore: bump version to <NEXT_VERSION>-SNAPSHOT"
```
