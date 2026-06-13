#!/bin/bash
# /opt/stacks/swtp/review-teardown.sh <namespace> <pr-number>
#
# Removes containers and images for a PR review environment.
# Called automatically when a PR is closed or merged.

set -e

NAMESPACE="$1"
PR="$2"
REGISTRY="ghcr.io/${NAMESPACE}"
LOG="/opt/stacks/swtp/deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [PR-${PR}] $1" >> "$LOG"; }

if [ -z "$NAMESPACE" ] || [ -z "$PR" ]; then
  echo "Usage: review-teardown.sh <namespace> <pr-number>" >&2
  exit 1
fi

log "Teardown triggered"

sudo docker rm -f "swtp-web-pr-${PR}" 2>/dev/null \
  && log "Frontend container removed" \
  || log "Frontend container not found (already gone?)"

sudo docker rm -f "swtp-api-pr-${PR}" 2>/dev/null \
  && log "Backend container removed" \
  || log "Backend container not found (already gone?)"

# Clean up PR-tagged images to free disk space
sudo docker rmi "${REGISTRY}/swtp-web:pr-${PR}" 2>/dev/null || true
sudo docker rmi "${REGISTRY}/swtp-api:pr-${PR}" 2>/dev/null || true

log "Teardown complete"
echo "----------------------------------------" >> "$LOG"
