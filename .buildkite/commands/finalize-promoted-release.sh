#!/usr/bin/env bash

set -eu

# Finalizes a promoted release once the developer has confirmed the block step and the preceding
# promote step submitted the build to Play: creates a draft GitHub release tagged at the commit the
# build came from, and opens the trunk version-name bump PR. The version code is passed as the first
# argument; the Yes/No confirmation is read from Buildkite meta-data. Runs on `mac-metal` because it
# writes git (tag + PR) and needs the bot identity from `use-bot-for-git`.

VERSION_CODE="${1:-}"

# `production_ready_to_release` must stay in sync with PRODUCTION_CONFIRM_META_DATA_KEY in
# fastlane/lanes/promote.rb, which is the key the block-step confirmation field writes.
READY="$(buildkite-agent meta-data get "production_ready_to_release" --default "no")"

if [[ "${READY}" != "yes" ]]; then
  echo "--- :no_entry_sign: Release not confirmed (production_ready_to_release=${READY}); skipping finalize."
  exit 0
fi

if [[ -z "${VERSION_CODE}" ]]; then
  echo "+++ :x: No version code was provided to finalize."
  exit 1
fi

echo "--- :robot_face: Use bot for git operations"
source use-bot-for-git

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :checkered_flag: Finalizing promoted release for ${VERSION_CODE}"
bundle exec fastlane finalize_promoted_release version_code:"${VERSION_CODE}"
