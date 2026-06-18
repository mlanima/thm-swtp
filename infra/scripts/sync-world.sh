#!/bin/bash
set -e

echo "→ Fetching and pruning..."
git fetch --prune

# 1. lokale branches löschen, deren upstream weg ist
echo "→ Removing branches with deleted upstream..."
git branch -vv | grep ': gone]' | awk '{print $1}' | xargs -r git branch -D

# 2. neue remote branches lokal tracken
echo "→ Tracking new remote branches..."
git branch -r | grep -v '\->' | sed 's|origin/||' | while read branch; do
    if ! git show-ref --verify --quiet "refs/heads/$branch"; then
        git branch --track "$branch" "origin/$branch"
        echo "  tracked: $branch"
    fi
done

# 3. alle fast-forwarden
echo "→ Fast-forwarding behind branches..."
current=$(git branch --show-current)
git branch -vv | grep '\[origin/' | grep -v ': gone]' | awk '{print $1}' | while read branch; do
    if [ "$branch" != "$current" ]; then
        git fetch origin "$branch:$branch" 2>/dev/null &&
            echo "  updated: $branch" ||
            echo "  skipped (diverged): $branch"
    else
        git pull --ff-only &&
            echo "  updated (current): $branch" ||
            echo "  skipped (not ff-able): $branch"
    fi
done

echo "→ Done."
