## ─── Configuration ────────────────────────────────────────────────────────────
GITHUB_OWNER  := sushantgawali
BACKEND_REPO  := shopizer
ADMIN_REPO    := shopizer-admin
SHOP_REPO     := shopizer-shop-reactjs

ARTIFACTS_DIR := .artifacts

COLIMA_CPU    ?= 4
COLIMA_MEM    ?= 8
COLIMA_DISK   ?= 60

.PHONY: all up down clean \
        start-colima stop-colima \
        fetch-artifacts extract-artifacts \
        build-images \
        tf-init tf-import-existing tf-apply tf-destroy

## ─── Top-level targets ────────────────────────────────────────────────────────

## Full bring-up: Colima → fetch → build → terraform apply
up: start-colima build-images tf-apply
	@echo ""
	@echo "═══════════════════════════════════════"
	@echo "  Shopizer is running on Colima k8s"
	@echo "═══════════════════════════════════════"
	@terraform -chdir=terraform output
	@echo ""

## Tear down k8s resources and stop Colima
down: tf-destroy stop-colima

## ─── Colima ───────────────────────────────────────────────────────────────────

start-colima:
	@if colima status 2>/dev/null | grep -q Running; then \
		echo "Colima is already running."; \
	else \
		echo "Starting Colima (CPU=$(COLIMA_CPU) MEM=$(COLIMA_MEM)GB DISK=$(COLIMA_DISK)GB) …"; \
		colima start --kubernetes --cpu $(COLIMA_CPU) --memory $(COLIMA_MEM) --disk $(COLIMA_DISK); \
	fi

stop-colima:
	colima stop

## ─── Artifacts ────────────────────────────────────────────────────────────────

$(ARTIFACTS_DIR):
	mkdir -p $(ARTIFACTS_DIR)

## Download release artifacts from each GitHub repo's latest release
fetch-artifacts: $(ARTIFACTS_DIR)
	@echo "→ Fetching shopizer.jar …"
	curl -fSL -o $(ARTIFACTS_DIR)/shopizer.jar \
		https://github.com/$(GITHUB_OWNER)/$(BACKEND_REPO)/releases/latest/download/shopizer.jar
	@echo "→ Fetching shopizer-admin.tar.gz …"
	curl -fSL -o $(ARTIFACTS_DIR)/shopizer-admin.tar.gz \
		https://github.com/$(GITHUB_OWNER)/$(ADMIN_REPO)/releases/latest/download/shopizer-admin.tar.gz
	@echo "→ Fetching shopizer-react-build.tar.gz …"
	curl -fSL -o $(ARTIFACTS_DIR)/shopizer-react-build.tar.gz \
		https://github.com/$(GITHUB_OWNER)/$(SHOP_REPO)/releases/latest/download/shopizer-react-build.tar.gz

## Extract artifacts into Docker build contexts
extract-artifacts: fetch-artifacts
	@echo "→ Staging backend JAR …"
	cp $(ARTIFACTS_DIR)/shopizer.jar docker/backend/shopizer.jar
	@echo "→ Extracting admin dist/ …"
	rm -rf docker/admin/dist && mkdir -p docker/admin/dist
	tar -xzf $(ARTIFACTS_DIR)/shopizer-admin.tar.gz -C docker/admin/dist
	@echo "→ Extracting shop build/ …"
	rm -rf docker/shop/build && mkdir -p docker/shop/build
	tar -xzf $(ARTIFACTS_DIR)/shopizer-react-build.tar.gz -C docker/shop/build

## ─── Docker images ────────────────────────────────────────────────────────────

build-images: extract-artifacts
	docker build -t shopizer-backend:latest docker/backend/
	docker build -t shopizer-admin:latest   docker/admin/
	docker build -t shopizer-shop:latest    docker/shop/

## ─── Terraform ────────────────────────────────────────────────────────────────

tf-init:
	terraform -chdir=terraform init

## Import pre-existing k8s deployments that exist in cluster but not in TF state
tf-import-existing: tf-init
	@set -e; \
	for entry in \
		"kubernetes_deployment.admin:shopizer-admin" \
		"kubernetes_deployment.backend:shopizer-backend" \
		"kubernetes_deployment.shop:shopizer-shop"; do \
		addr=$${entry%%:*}; \
		name=$${entry##*:}; \
		if terraform -chdir=terraform state show $$addr >/dev/null 2>&1; then \
			echo "✓ $$addr already tracked in Terraform state"; \
		elif kubectl get deployment $$name -n shopizer >/dev/null 2>&1; then \
			echo "→ Importing existing deployment $$name into $$addr"; \
			terraform -chdir=terraform import $$addr shopizer/$$name; \
		else \
			echo "• deployment $$name not found in cluster; Terraform will create it"; \
		fi; \
	done

tf-apply: tf-import-existing
	terraform -chdir=terraform apply -auto-approve

tf-destroy:
	terraform -chdir=terraform destroy -auto-approve

## ─── Helpers ──────────────────────────────────────────────────────────────────

## Remove downloaded artifacts and staged Docker build contexts
clean:
	rm -rf $(ARTIFACTS_DIR) \
	       docker/backend/shopizer.jar \
	       docker/admin/dist/ \
	       docker/shop/build/

## Show status of all pods
status:
	kubectl get pods,svc -n shopizer

## Tail backend logs
logs-backend:
	kubectl logs -n shopizer -l app=shopizer-backend -f

## Tail admin logs
logs-admin:
	kubectl logs -n shopizer -l app=shopizer-admin -f

## Tail shop logs
logs-shop:
	kubectl logs -n shopizer -l app=shopizer-shop -f
