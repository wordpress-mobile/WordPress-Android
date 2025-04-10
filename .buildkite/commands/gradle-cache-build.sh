#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
if [ "$1" = "wordpress" ]; then
  ./gradlew assembleWordpressWasabiDebug -Dorg.gradle.caching.debug=true --rerun-tasks --console=plain
fi

if [ "$1" = "jetpack" ]; then
  ./gradlew assembleJetpackWasabiDebug -Dorg.gradle.caching.debug=true
fi

find libs/ -type f -name "annotations.jar" -exec cp {} . \;
find libs/ -type f -name "fluxc-annotations.jar" -exec cp {} . \;
