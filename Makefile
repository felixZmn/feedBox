.PHONY: release help release-dry-run check-clean remove-snapshot build-java build-docker push-docker build-helm increment-version git-commit-version

# Configuration
DOCKER_REGISTRY := ghcr.io/felixzmn/docker
HELM_REGISTRY := oci://ghcr.io/felixzmn/helm
GIT_REMOTE := origin
MAIN_BRANCH := main

# Extract version from pom.xml (source of truth)
CURRENT_VERSION := $(shell grep -m 1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
VERSION := $(CURRENT_VERSION:-SNAPSHOT=)
NEXT_VERSION := $(shell echo $(VERSION) | awk -F. '{$$NF=$$NF+1; print}' OFS='.')-SNAPSHOT
APP_NAME := $(shell grep -m 1 '<artifactId>' pom.xml | sed 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/')

help:
	@echo "Available targets:"
	@echo "  make release          - Execute full release process"
	@echo "  make release-dry-run  - Show what would be done"
	@echo ""
	@echo "Current version: $(CURRENT_VERSION)"
	@echo "Release version: $(VERSION)"
	@echo "Next version:    $(NEXT_VERSION)"
	@echo ""

release: check-clean remove-snapshot git-commit-release build-java build-docker push-docker build-helm increment-version git-commit-version
	@echo "  Release Version: $(VERSION)"
	@echo "  Docker Image:    $(DOCKER_REGISTRY):$(VERSION)"
	@echo "  Docker Latest:   $(DOCKER_REGISTRY):latest"
	@echo "  Helm Chart:      $(HELM_REGISTRY)"
	@echo "  Next Version:    $(NEXT_VERSION)"
	@echo ""

release-dry-run:
	@echo "Current pom version:   $(CURRENT_VERSION)"
	@echo "Release version:       $(VERSION)"
	@echo "Next version:          $(NEXT_VERSION)"
	@echo ""
	@echo "Steps that would execute:"
	@echo "  1. Remove -SNAPSHOT from pom.xml and Chart.yaml"
	@echo "  2. Commit release version"
	@echo "  3. Build Java app with Maven"
	@echo "  4. Build Docker image: $(DOCKER_REGISTRY):$(VERSION)"
	@echo "  5. Push Docker images ($(VERSION) and latest)"
	@echo "  6. Package and push Helm chart to $(HELM_REGISTRY)"
	@echo "  7. Increment version to $(NEXT_VERSION)"
	@echo "  8. Commit next version"
	@echo ""

# Step 0: Check git is clean
check-clean:
	@echo "Step 0/8: Checking git status..."
	@if [ -n "$$(git status --porcelain)" ]; then \
        echo "✗ ERROR: Git working directory is not clean"; \
        echo ""; \
        git status; \
        exit 1; \
    fi
	@echo "✓ Git is clean"

# Step 1: Remove -SNAPSHOT from pom.xml and Chart.yaml
remove-snapshot:
	@echo "Step 1/8: Removing -SNAPSHOT from pom.xml..."
	@sed -i.bak 's/<version>$(CURRENT_VERSION)<\/version>/<version>$(VERSION)<\/version>/' pom.xml || exit 1
	@rm pom.xml.bak
	@echo "Step 1/8: Removing -SNAPSHOT from Chart.yaml..."
	@sed -i.bak 's/version: $(CURRENT_VERSION)/version: $(VERSION)/' chart/Chart.yaml || exit 1
	@sed -i.bak 's/appVersion: $(CURRENT_VERSION)/appVersion: $(VERSION)/' chart/Chart.yaml || exit 1
	@rm chart/Chart.yaml.bak
	@git add pom.xml chart/Chart.yaml
	@echo "✓ Snapshots removed"

# Step 2: Commit release version
git-commit-release:
	@echo "Step 2/8: Committing release version $(VERSION)..."
	@git commit -m "chore(release): $(VERSION)" || exit 1
	@git tag -a $(VERSION) -m "chore(release): $(VERSION)" || exit 1
	@echo "✓ Version $(VERSION) committed and tagged"

# Step 3: Build Java app with Maven
build-java:
	@echo "Step 3/8: Building Java application with Maven..."
	@mvn clean package -DskipTests || exit 1
	@echo "✓ Java build successful"

# Step 4: Build Docker image
build-docker:
	@echo "Step 4/8: Building Docker image..."
	@docker build -t $(DOCKER_REGISTRY):$(VERSION) -t $(DOCKER_REGISTRY):latest . || exit 1
	@echo "✓ Docker image built: $(DOCKER_REGISTRY):$(VERSION)"

# Step 5: Push Docker images
push-docker: build-docker
	@echo "Step 5/8: Pushing Docker images..."
	@docker push $(DOCKER_REGISTRY):$(VERSION) || exit 1
	@docker push $(DOCKER_REGISTRY):latest || exit 1
	@echo "✓ Docker images pushed"

# Step 6: Package and push Helm chart
build-helm:
	@echo "Step 6/8: Packaging and pushing Helm chart..."
	@helm package chart/ --version $(VERSION) || exit 1
	@helm push $(APP_NAME)-$(VERSION).tgz $(HELM_REGISTRY) || exit 1
	@rm -f $(APP_NAME)-$(VERSION).tgz
	@echo "✓ Helm chart pushed to $(HELM_REGISTRY)"

# Step 7: Increment version and add -SNAPSHOT
increment-version:
	@echo "Step 7/8: Incrementing versions..."
	@sed -i.bak 's/<version>$(VERSION)<\/version>/<version>$(NEXT_VERSION)<\/version>/' pom.xml || exit 1
	@rm pom.xml.bak
	@NEXT_CHART_VERSION=$$(echo $(VERSION) | awk -F. '{$$NF=$$NF+1; OFS="."; print $$0}'); \
	@sed -i.bak "s/version: $(VERSION)/version: $$NEXT_CHART_VERSION/" chart/Chart.yaml || exit 1
	@sed -i.bak 's/appVersion: $(VERSION)/appVersion: $(NEXT_VERSION)/' chart/Chart.yaml || exit 1
	@rm chart/Chart.yaml.bak
	@git add pom.xml chart/Chart.yaml
	@echo "✓ pom.xml version incremented to $(NEXT_VERSION)"
	@echo "✓ Chart appVersion incremented to $(NEXT_VERSION)"
	@echo "✓ Chart version incremented independently"

# Step 8: Commit next version
git-commit-version:
	@echo "Step 8/8: Committing next version $(NEXT_VERSION)..."
	@git commit -m "chore(release): bump version to $(NEXT_VERSION)" || exit 1
	@echo "✓ Next version committed"
	@echo ""
	@echo "Pushing to $(GIT_REMOTE)/$(MAIN_BRANCH)..."
	@git push $(GIT_REMOTE) $(MAIN_BRANCH) --tags || exit 1
	@echo "✓ Changes pushed"
