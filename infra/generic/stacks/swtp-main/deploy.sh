#!/bin/bash
# /opt/stacks/swtp-main/deploy.sh
# Pulls :latest images and restarts the swtp-main stack.

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
LOG="/opt/stacks/swtp-main/deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG"; }

log "Deploy triggered (swtp-main)"

# Pull images
PULL=$(docker compose -f "$DIR/docker-compose.yml" pull 2>&1)

# Summarize per service
for svc in swtp-main-api swtp-main-web; do
  if echo "$PULL" | grep -q "$svc.*Downloaded newer image\|$svc.*Pulled"; then
    log "$svc: updated"
  elif echo "$PULL" | grep -q "$svc.*Image is up to date\|$svc.*up-to-date"; then
    log "$svc: up-to-date"
  else
    log "$svc: check output"
  fi
done

log "Restarting services"
docker compose -f "$DIR/docker-compose.yml" up -d >> "$LOG" 2>&1
log "Deploy complete"
echo "----------------------------------------" >> "$LOG"
