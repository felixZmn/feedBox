.DEFAULT_GOAL := help
DRY_RUN       ?= 0

# ---------------------------------------------------------------------------
# Version extraction — evaluated once at parse time against current sources
# ---------------------------------------------------------------------------

APP_VERSION   := $(shell grep -m1 '<version>' pom.xml \
                   | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/' \
                   | sed 's/-SNAPSHOT//')

CHART_VERSION := $(shell grep '^version:' chart/Chart.yaml \
                   | sed -E 's/version:[[:space:]]*//' \
                   | sed 's/-SNAPSHOT//')

NEXT_APP_VERSION   := $(shell echo "$(APP_VERSION)" \
                        | awk -F. '{printf "%s.%s.%d\n", $$1, $$2, $$3+1}')

NEXT_CHART_VERSION := $(shell echo "$(CHART_VERSION)" \
                        | awk -F. '{printf "%s.%s.%d\n", $$1, $$2, $$3+1}')

DOCKER_IMAGE := ghcr.io/felixzmn/docker/feedbox

# ---------------------------------------------------------------------------
# Public targets
# ---------------------------------------------------------------------------

.PHONY: help release dry-run

help:
	@echo "Usage:"
	@echo "  make release   — full release: modify files, commit, build, push, bump to next snapshot, commit"
	@echo "  make dry-run   — same as release, but skip all git commits and docker/helm pushes"
	@echo ""
	@echo "Versions detected  (app: $(APP_VERSION), chart: $(CHART_VERSION))"
	@echo "Next snapshot will be (app: $(NEXT_APP_VERSION)-SNAPSHOT, chart: $(NEXT_CHART_VERSION)-SNAPSHOT)"

release:
	@$(MAKE) --no-print-directory _release DRY_RUN=0

dry-run:
	@$(MAKE) --no-print-directory _release DRY_RUN=1

# ---------------------------------------------------------------------------
# Internal release pipeline
# ---------------------------------------------------------------------------

.PHONY: _release _prepare _commit-release _build-docker _push-docker \
        _build-helm _push-helm _bump _commit-snapshot

_release: _prepare _commit-release _build-docker _push-docker _build-helm _push-helm _bump _commit-snapshot
	@echo ""
	@echo "Done. Released $(APP_VERSION), next development version is $(NEXT_APP_VERSION)-SNAPSHOT."
ifeq ($(DRY_RUN),1)
	@echo "(dry-run mode — no commits or pushes were made)"
endif

# Step 1 — strip -SNAPSHOT suffix from all version fields
_prepare:
	@echo ""
	@echo "==> [1/5] Preparing release $(APP_VERSION)"
	@echo "    pom.xml           : $(APP_VERSION)-SNAPSHOT -> $(APP_VERSION)"
	@echo "    chart appVersion  : $(APP_VERSION)-SNAPSHOT -> $(APP_VERSION)"
	@echo "    chart version     : $(CHART_VERSION)-SNAPSHOT -> $(CHART_VERSION)"
	sed -i 's|<version>$(APP_VERSION)-SNAPSHOT</version>|<version>$(APP_VERSION)</version>|' pom.xml
	sed -i 's|appVersion: "$(APP_VERSION)-SNAPSHOT"|appVersion: "$(APP_VERSION)"|' chart/Chart.yaml
	sed -i 's|^version: $(CHART_VERSION)-SNAPSHOT|version: $(CHART_VERSION)|' chart/Chart.yaml

# Step 2 — commit the release
_commit-release:
	@echo ""
	@echo "==> [2/5] Committing release"
ifeq ($(DRY_RUN),1)
	@echo "    [DRY RUN] skipping: git add pom.xml chart/Chart.yaml"
	@echo "    [DRY RUN] skipping: git commit -m \"chore: release $(APP_VERSION)\""
else
	git add pom.xml chart/Chart.yaml
	git commit -m "chore: release $(APP_VERSION)"
endif

# Step 3 — build the Docker image (always runs, even in dry-run)
_build-docker:
	@echo ""
	@echo "==> [3/5] Building Docker image $(DOCKER_IMAGE):$(APP_VERSION)"
	docker build \
	  -t $(DOCKER_IMAGE):$(APP_VERSION) \
	  -t $(DOCKER_IMAGE):latest \
	  .

# Push is skipped in dry-run
_push-docker:
ifeq ($(DRY_RUN),1)
	@echo "    [DRY RUN] skipping: docker push $(DOCKER_IMAGE):$(APP_VERSION)"
	@echo "    [DRY RUN] skipping: docker push $(DOCKER_IMAGE):latest"
else
	docker push $(DOCKER_IMAGE):$(APP_VERSION)
	docker push $(DOCKER_IMAGE):latest
endif

# Step 4 — package the Helm chart (always runs, even in dry-run)
_build-helm:
	@echo ""
	@echo "==> [4/5] Packaging Helm chart v$(CHART_VERSION)"
	cd chart && helm package .

# Push is skipped in dry-run
_push-helm:
ifeq ($(DRY_RUN),1)
	@echo "    [DRY RUN] skipping: helm push feedbox-$(CHART_VERSION).tgz oci://ghcr.io/felixzmn/helm"
else
	cd chart && helm push feedbox-$(CHART_VERSION).tgz oci://ghcr.io/felixzmn/helm
endif

# Step 5 — bump to next patch snapshot
_bump:
	@echo ""
	@echo "==> [5/5] Bumping to next development version"
	@echo "    pom.xml           : $(APP_VERSION) -> $(NEXT_APP_VERSION)-SNAPSHOT"
	@echo "    chart appVersion  : $(APP_VERSION) -> $(NEXT_APP_VERSION)-SNAPSHOT"
	@echo "    chart version     : $(CHART_VERSION) -> $(NEXT_CHART_VERSION)-SNAPSHOT"
	sed -i 's|<version>$(APP_VERSION)</version>|<version>$(NEXT_APP_VERSION)-SNAPSHOT</version>|' pom.xml
	sed -i 's|appVersion: "$(APP_VERSION)"|appVersion: "$(NEXT_APP_VERSION)-SNAPSHOT"|' chart/Chart.yaml
	sed -i 's|^version: $(CHART_VERSION)|version: $(NEXT_CHART_VERSION)-SNAPSHOT|' chart/Chart.yaml

_commit-snapshot:
ifeq ($(DRY_RUN),1)
	@echo "    [DRY RUN] skipping: git add pom.xml chart/Chart.yaml"
	@echo "    [DRY RUN] skipping: git commit -m \"chore: bump version to $(NEXT_APP_VERSION)-SNAPSHOT\""
else
	git add pom.xml chart/Chart.yaml
	git commit -m "chore: bump version to $(NEXT_APP_VERSION)-SNAPSHOT"
endif
