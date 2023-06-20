#!/bin/bash -eu

export DANGER_GITHUB_API_TOKEN="$GITHUB_TOKEN"

echo "--- :rubygems: Setting up Gems"
bundle install

echo "--- Running Danger"
bundle exec danger --dangerfile=.buildkite/danger/Dangerfile-pr-check --danger_id=pr-check
