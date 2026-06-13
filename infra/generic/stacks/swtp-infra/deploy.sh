#!/bin/bash
# /opt/stacks/swtp-infra/deploy.sh
# Starts the swtp-infra stack (MySQL, Keycloak, Hoppscotch).
# No image pull needed — all images are public.

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
LOG="$DIR/deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG"; }

log "Deploy triggered (swtp-infra)"
docker compose -f "$DIR/docker-compose.yml" up -d >> "$LOG" 2>&1
log "Deploy complete"
echo "----------------------------------------" >> "$LOG"
