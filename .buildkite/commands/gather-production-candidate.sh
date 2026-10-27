#!/usr/bin/env bash

set -eu

# Determines the current beta build, opens the "confirm the release" block step, and posts to
# Slack. No build — just gems + secrets.

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :android: Gathering the production candidate and opening the block step"
bundle exec fastlane gather_production_candidate
