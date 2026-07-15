#!/usr/bin/env bash

set -eu

# Promotes the build chosen in the preceding block step to the beta track. No build — just gems + secrets.

# `beta_build_to_promote` must stay in sync with PROMOTION_META_DATA_KEY in fastlane/lanes/promote.rb,
# which is the key the gather lane writes the block-step select field under.
VERSION_CODE="$(buildkite-agent meta-data get "beta_build_to_promote" --default "")"

if [[ -z "${VERSION_CODE}" ]]; then
  echo "+++ :x: No build was selected to promote."
  exit 1
fi

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :rocket: Promoting ${VERSION_CODE} to the beta track"
bundle exec fastlane promote_to_beta version_code:"${VERSION_CODE}"
