#!/usr/bin/env bash
#
# Rebase this fork's branch onto upstream mLauncher.
#
# Usage: tools/sync-upstream.sh [fork-branch] [upstream-branch]
#   defaults: current branch, main
#
# The script only fetches and rebases; it never pushes. See FORK.md for what to do
# when a conflict lands, and for the list of files this fork actually touches.

set -euo pipefail

UPSTREAM_URL="https://github.com/CodeWorksCreativeHub/mLauncher.git"
FORK_BRANCH="${1:-$(git rev-parse --abbrev-ref HEAD)}"
UPSTREAM_BRANCH="${2:-main}"

if [ -n "$(git status --porcelain)" ]; then
    echo "Working tree is dirty. Commit or stash first." >&2
    exit 1
fi

# Replaying the same conflict resolution on every future rebase.
git config rerere.enabled true

if ! git remote get-url upstream >/dev/null 2>&1; then
    echo "Adding upstream remote: $UPSTREAM_URL"
    git remote add upstream "$UPSTREAM_URL"
fi

echo "Fetching upstream/$UPSTREAM_BRANCH ..."
for attempt in 1 2 3 4; do
    if git fetch upstream "$UPSTREAM_BRANCH"; then
        break
    fi
    if [ "$attempt" = "4" ]; then
        echo "Could not fetch upstream after 4 attempts." >&2
        exit 1
    fi
    sleep $((2 ** attempt))
done

BACKUP="backup/${FORK_BRANCH}-$(date +%Y%m%d-%H%M%S)"
git branch "$BACKUP" "$FORK_BRANCH"
echo "Backed up $FORK_BRANCH to $BACKUP"

echo "Commits this fork carries on top of upstream:"
git log --oneline "upstream/$UPSTREAM_BRANCH..$FORK_BRANCH"

git checkout "$FORK_BRANCH"

if git rebase "upstream/$UPSTREAM_BRANCH"; then
    echo
    echo "Rebased cleanly onto upstream/$UPSTREAM_BRANCH."
    echo "Files this fork still changes in upstream code:"
    git diff --stat "upstream/$UPSTREAM_BRANCH" -- \
        app/src/main/java/com/github/codeworkscreativehub/mlauncher \
        app/src/main/res
    echo
    echo "Next: ./gradlew assembleProdDebug   (CI builds assembleProdRelease)"
    echo "Then verify the checklist in FORK.md before force pushing."
else
    echo
    echo "Rebase stopped on a conflict. FORK.md lists every anchor this fork depends on."
    echo "Resolve, then: git rebase --continue   (or: git rebase --abort)"
    echo "Your pre-rebase state is on branch $BACKUP"
    exit 1
fi
