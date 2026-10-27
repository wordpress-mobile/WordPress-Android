#!/usr/bin/env bash

set -eu

# Advances the live production staged rollout one step — reads the current rollout percentage back
# from Play and bumps WordPress + Jetpack to the next step, finalizing to 100% past the top. The lane
# discovers everything from Play, so there are no arguments. No build — just gems + secrets.

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :chart_with_upwards_trend: Advancing the production rollout"
bundle exec fastlane advance_production_rollout
