#!/bin/bash
# /opt/stacks/swtp/dispatch.sh
#
# Forced command for the deploy SSH key in authorized_keys:
#   command="/opt/stacks/swtp/dispatch.sh" ssh-ed25519 ...
#
# Dispatches based on SSH_ORIGINAL_COMMAND:
#   (no command)                     → deploy-dev (default)
#   deploy-dev                       → deploy swtp-dev stack
#   deploy-main                      → deploy swtp-main stack
#   review-deploy <namespace> <pr>   → spin up review environment
#   review-teardown <namespace> <pr> → tear down review environment

set -e

CMD="${SSH_ORIGINAL_COMMAND:-deploy-dev}"

case "$CMD" in
  deploy-dev)
    exec /opt/stacks/swtp/deploy.sh swtp-dev
    ;;
  deploy-main)
    exec /opt/stacks/swtp/deploy.sh swtp-main
    ;;
  "review-deploy "*)
    ARGS="${CMD#review-deploy }"
    exec /opt/stacks/swtp/review-deploy.sh $ARGS
    ;;
  "review-teardown "*)
    ARGS="${CMD#review-teardown }"
    exec /opt/stacks/swtp/review-teardown.sh $ARGS
    ;;
  *)
    echo "Unknown command: $CMD" >&2
    exit 1
    ;;
esac
