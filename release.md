# Release Process

Versions follow SemVer (`MAJOR.MINOR.PATCH`). **Git tags are the source of truth.**  
`pom.xml` keeps a placeholder `0.0.0-SNAPSHOT` via `${revision}`; CI sets the real version only for the release build.

## Automated flow

1. Renovate opens dependency PRs. Non-major (patch/minor) updates are grouped and **automerge** once CI is green. Majors stay manual.
2. Every merge to `main` runs [`.github/workflows/release.yml`](.github/workflows/release.yml).
3. The workflow derives the next version from conventional commits since the latest tag:
   - `fix:` / anything else that is not feat/breaking → **patch**
   - `feat:` / `feat(scope):` → **minor**
   - `BREAKING CHANGE:` or `type!:` → **major**
4. It builds the Quarkus native binary, pushes:
   - `ghcr.io/felixzmn/docker/feedbox:X.Y.Z`
   - `ghcr.io/felixzmn/docker/feedbox:latest`
5. It creates the git tag and a GitHub Release.
6. It opens a PR in [`felixZmn/feedbox-chart`](https://github.com/felixZmn/feedbox-chart) bumping `appVersion` / chart `version` (requires `RENOVATE_PAT` with access to that repo).

## Feature development

1. Branch from `main` (do **not** edit versions in `pom.xml`).
2. Use conventional commit subjects (`feat:`, `fix:`, `chore:`, …).
3. Open a PR → `Test backend` must pass.
4. Merge to `main` → release workflow publishes the new tag and image.

If you **squash-merge**, keep a conventional type in the squash commit subject (or PR title, if that becomes the squash message).

### Hotfix

PR with `fix:` into `main` → automatic **patch** release.

### Breaking / major

Use `feat!:` / `fix!:` or a `BREAKING CHANGE:` footer. Do not automerge Renovate majors; merge those only when you intend a major bump.

## Manual / forced release

Run **Release** via `workflow_dispatch`:

- Leave **version** empty to derive SemVer from commits, or
- Set an explicit version (e.g. `3.7.0`) to force that tag.

Re-runs are no-ops if the tag already exists.

## Local native build (optional)

```bash
mvn package -Dnative -Dquarkus.native.container-build=true -Drevision=0.0.0-SNAPSHOT
docker build -f src/main/docker/Dockerfile.native-micro -t feedbox:local .
```

## Chart / deploy

After an app release, merge the automated chart PR, then tag the chart repo with the chart `version` so [`feedbox-chart` release](https://github.com/felixZmn/feedbox-chart/blob/main/.github/workflows/release.yml) publishes the Helm chart to GHCR.

## Required repository setup

- Branch protection on `main`: require the **Test backend** check so Renovate automerge only lands green PRs.
- Secret `RENOVATE_PAT`: GitHub PAT that can open PRs on `felixZmn/feedbox-chart` (and run Renovate on this repo).
- Workflow permissions: `contents: write` and `packages: write` for tagging and GHCR pushes.
