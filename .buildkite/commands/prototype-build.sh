#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane "build_and_upload_$1_prototype_build"
