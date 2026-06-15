#!/bin/bash
# /opt/stacks/swtp-infra/review-deploy.sh <namespace> <pr-number>
#
# Spins up per-PR containers (web + api) routed via Traefik.
# URLs:
#   Frontend: https://pr-<n>.review.swtp-ss26.de
#   Backend:  https://pr-<n>-api.review.swtp-ss26.de

set -e

NAMESPACE="$1"
PR="$2"

# ── Config ────────────────────────────────────────────────────────────────────
REGISTRY="ghcr.io/${NAMESPACE}"
TRAEFIK_NETWORK="traefik-net"
CERTRESOLVER="letsencrypt-inwx"
DOMAIN="review.swtp-ss26.de"
LOG="/opt/stacks/swtp-infra/deploy.log"
# ──────────────────────────────────────────────────────────────────────────────

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [PR-${PR}] $1" >> "$LOG"; }

if [ -z "$NAMESPACE" ] || [ -z "$PR" ]; then
  echo "Usage: review-deploy.sh <namespace> <pr-number>" >&2
  exit 1
fi

log "Review deploy triggered (namespace: $NAMESPACE)"

deploy_web() {
  local name="swtp-web-pr-${PR}"
  local host="pr-${PR}.${DOMAIN}"

  log "Pulling frontend image..."
  sudo docker pull "${REGISTRY}/swtp-web:pr-${PR}"

  log "Starting frontend container..."
  sudo docker rm -f "$name" 2>/dev/null || true
  sudo docker run -d \
    --name "$name" \
    --network "$TRAEFIK_NETWORK" \
    --restart unless-stopped \
    --label "traefik.enable=true" \
    --label "traefik.http.routers.${name}.entrypoints=websecure" \
    --label "traefik.http.routers.${name}.rule=Host(\`${host}\`)" \
    --label "traefik.http.routers.${name}.tls=true" \
    --label "traefik.http.routers.${name}.tls.certresolver=${CERTRESOLVER}" \
    --label "traefik.http.routers.${name}.tls.domains[0].main=*.${DOMAIN}" \
    --label "traefik.http.services.${name}.loadbalancer.server.port=4000" \
    "${REGISTRY}/swtp-web:pr-${PR}"

  log "Frontend live → https://${host}"
}

deploy_api() {
  local name="swtp-api-pr-${PR}"
  local host="pr-${PR}-api.${DOMAIN}"

  log "Pulling backend image..."
  sudo docker pull "${REGISTRY}/swtp-api:pr-${PR}"

  log "Starting backend container..."
  sudo docker rm -f "$name" 2>/dev/null || true
  sudo docker run -d \
    --name "$name" \
    --network "$TRAEFIK_NETWORK" \
    --network review_net \
    --restart unless-stopped \
    --env-file /opt/stacks/swtp-infra/review.env \
    -e "SPRING_DATASOURCE_URL=jdbc:mysql://swtp-db:3306/swtp_pr_${PR}" \
    --label "traefik.enable=true" \
    --label "traefik.http.routers.${name}.entrypoints=websecure" \
    --label "traefik.http.routers.${name}.rule=Host(\`${host}\`)" \
    --label "traefik.http.routers.${name}.tls=true" \
    --label "traefik.http.routers.${name}.tls.certresolver=${CERTRESOLVER}" \
    --label "traefik.http.routers.${name}.tls.domains[0].main=*.${DOMAIN}" \
    --label "traefik.http.services.${name}.loadbalancer.server.port=8080" \
    "${REGISTRY}/swtp-api:pr-${PR}"

  log "Backend live → https://${host}"
}

deploy_web
deploy_api

log "Review deploy complete"
echo "----------------------------------------" >> "$LOG"
