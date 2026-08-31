#!/usr/bin/env bash

set -eu

# Promotes the build chosen in the preceding block step to the beta track. No build — just gems + secrets.

# `beta_build_to_promote` must stay in sync with BETA_META_DATA_KEY in fastlane/lanes/promote.rb,
# which is the key the gather lane writes the block-step build select field under.
VERSION_CODE="$(buildkite-agent meta-data get "beta_build_to_promote" --default "")"

# `beta_release_notes_option` must stay in sync with BETA_RELEASE_NOTES_META_DATA_KEY in the same file
# (the block step's release-notes select field).
RELEASE_NOTES_OPTION="$(buildkite-agent meta-data get "beta_release_notes_option" --default "")"

if [[ -z "${VERSION_CODE}" ]]; then
  echo "+++ :x: No build was selected to promote."
  exit 1
fi

if [[ -z "${RELEASE_NOTES_OPTION}" ]]; then
  echo "+++ :x: No release notes option was selected."
  exit 1
fi

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :rocket: Promoting ${VERSION_CODE} to the beta track"
bundle exec fastlane promote_to_beta version_code:"${VERSION_CODE}" release_notes_option:"${RELEASE_NOTES_OPTION}"
