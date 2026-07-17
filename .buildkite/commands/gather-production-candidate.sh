#!/usr/bin/env bash

set -eu

# Determines the current beta build, opens the "confirm the release" block step, and posts to
# Slack. No build — just gems + secrets.

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :android: Gathering the production candidate and opening the block step"
bundle exec fastlane gather_production_candidate
