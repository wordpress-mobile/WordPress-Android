#!/bin/bash -eu

export DANGER_GITHUB_API_TOKEN="$GITHUB_TOKEN"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- Env Vars"
printenv

echo "--- Running Danger: PR Check"
bundle exec danger --fail-on-errors=true --dangerfile=.buildkite/danger/Dangerfile-pr-check --danger_id=pr-check
