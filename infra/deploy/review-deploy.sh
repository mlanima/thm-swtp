#!/bin/bash
# /opt/stacks/swtp/review-deploy.sh <pr-number> [api] [web]
#
# Spins up per-PR containers routed via Traefik.
# URLs:
#   Frontend: https://pr-<n>.review.swtp-ss26.de
#   Backend:  https://pr-<n>-api.review.swtp-ss26.de
#
# Prerequisites on the server:
#   - Traefik running with a certresolver that supports DNS-01 (for wildcard certs)
#   - *.review.swtp-ss26.de DNS A-record pointing to this server
#   - Docker network TRAEFIK_NETWORK exists and Traefik is attached to it
#   - /opt/stacks/swtp/review.env with runtime env vars for the backend
#     (DB connection etc. — can point at the dev DB)

set -e

PR="$1"
shift
SERVICES="$*"   # e.g. "api web" or just "api" or "web"

# ── Config ────────────────────────────────────────────────────────────────────
TRAEFIK_NETWORK="traefik"          # docker network Traefik listens on
CERTRESOLVER="letsencrypt"         # name of your Traefik certresolver
DOMAIN="review.swtp-ss26.de"
REGISTRY="ghcr.io/mlanima"
LOG="/opt/stacks/swtp/deploy.log"
# ──────────────────────────────────────────────────────────────────────────────

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [PR-${PR}] $1" >> "$LOG"; }

if [ -z "$PR" ] || [ -z "$SERVICES" ]; then
  echo "Usage: review-deploy.sh <pr-number> <api|web> [api|web]" >&2
  exit 1
fi

log "Review deploy triggered (services: $SERVICES)"

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
    --label "traefik.http.services.${name}.loadbalancer.server.port=80" \
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
    --restart unless-stopped \
    --env-file /opt/stacks/swtp/review.env \
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

for svc in $SERVICES; do
  case "$svc" in
    api) deploy_api ;;
    web) deploy_web ;;
    *)   log "Unknown service: $svc — skipped" ;;
  esac
done

log "Review deploy complete"
echo "----------------------------------------" >> "$LOG"
