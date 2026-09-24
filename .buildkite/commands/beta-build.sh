#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_beta app:$1 skip_confirm:true upload_to_play_store:true

echo "--- 💾 Saving Artifact"
for aab in build/*.aab; do
  buildkite-agent artifact upload "$aab"
  echo "<a href="artifact://$aab">$(basename "$aab")</a>" | buildkite-agent annotate --style info --context "beta-build-$aab"
done
