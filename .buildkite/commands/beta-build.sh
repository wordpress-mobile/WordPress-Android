#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_beta app:$1 skip_confirm:true skip_prechecks:true upload_to_play_store:true

echo "--- 💾 Saving Artifact"
for aab in build/*.aab; do
  buildkite-agent artifact upload "$aab"
  echo "<a href="artifact://$aab">$(basename "$aab")</a>" | buildkite-agent annotate --style info --context "beta-build-$aab"
done
