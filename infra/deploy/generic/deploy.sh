#!/bin/bash
# /opt/stacks/<stack>/deploy.sh <stack-name>
# Usage: deploy.sh swtp-dev   or   deploy.sh swtp-main
#
# Production deploy for a specific stack — pull images, restart services.

set -e

STACK="$1"
if [ -z "$STACK" ]; then
  echo "Usage: deploy.sh <stack-name>" >&2
  exit 1
fi

STACK_DIR="/opt/stacks/$STACK"
cd "$STACK_DIR" || exit 1
LOG="$STACK_DIR/deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG"; }

log "Deploy triggered (stack: $STACK)"

# Pull images
PULL=$(sudo docker compose pull 2>&1)

# Summarize per service found in compose file
SERVICES=$(sudo docker compose config --services 2>/dev/null)
for svc in $SERVICES; do
  if echo "$PULL" | grep -q "$svc.*Downloaded newer image\|$svc.*Pulled"; then
    log "$svc: updated"
  elif echo "$PULL" | grep -q "$svc.*Image is up to date\|$svc.*up-to-date"; then
    log "$svc: up-to-date"
  else
    log "$svc: check output"
  fi
done

log "Restarting services"
sudo docker compose up -d >> "$LOG" 2>&1
log "Deploy complete"
echo "----------------------------------------" >> "$LOG"
