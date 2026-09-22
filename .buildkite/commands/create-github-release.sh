#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- ⬇️ Downloading AAB Artefacts"
mkdir -p build/
buildkite-agent artifact download "*.aab" build/

echo "--- :github: Create GitHub Release"
bundle exec fastlane create_gh_release
