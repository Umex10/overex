#!/usr/bin/env bash
# =============================================================================
# Overex — run every CI workflow locally with act, in parallel
#
# Usage (from anywhere in the repository):
#   scripts/act-all.sh                       -> all workflows below
#   scripts/act-all.sh --concurrent-jobs 2   -> every argument goes to each act call
#   cd apps/web && npm run act:all           -> the same through npm
#
# Starts one "gh act push" per workflow in the background, waits for all of
# them and prints one line per workflow. Each full log goes to its own file
# (path printed at the start), so the terminal is not flooded. Exit code 1 as
# soon as one workflow failed.
#
# docker-publish is left out on purpose: act ignores its "branches: main"
# filter, and with a valid token it would really push an image to ghcr.io.
# docker-ci builds the same Dockerfile without pushing, which is the local
# check for it.
#
# Heavy: Maven, a Docker build and the e2e suite (database, Spring, browser)
# run at the same time.
# =============================================================================

set -u

# act resolves .github/workflows relative to the current folder.
cd "$(git rev-parse --show-toplevel)" || exit 1

workflows=(
  backend-verify-ci
  docker-ci
  web-checks
  workflow-lint
)

log_dir=$(mktemp -d -t overex-act-XXXXXX)
echo "Logs: $log_dir"

pids=()
for workflow in "${workflows[@]}"; do
  gh act push -W ".github/workflows/$workflow.yml" "$@" >"$log_dir/$workflow.log" 2>&1 &
  pids+=("$!")
  echo "started  $workflow"
done

# Waits in list order; the summary is complete once the slowest one is done.
failed=0
for i in "${!workflows[@]}"; do
  if wait "${pids[$i]}"; then
    echo "✅ ${workflows[$i]}"
  else
    echo "❌ ${workflows[$i]}  (log: $log_dir/${workflows[$i]}.log)"
    failed=1
  fi
done

exit $failed
