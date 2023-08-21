#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- ⬇️ Downloading AAB Artefacts"
mkdir -p build/
buildkite-agent artifact download "*.aab" build/

echo "--- :github: Create GitHub Release"
bundle exec fastlane create_gh_release
