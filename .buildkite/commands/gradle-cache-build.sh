#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
if [ "$1" = "wordpress" ]; then
  ./gradlew assembleWordpressWasabiDebug
fi

if [ "$1" = "jetpack" ]; then
  ./gradlew assembleJetpackWasabiDebug
fi
