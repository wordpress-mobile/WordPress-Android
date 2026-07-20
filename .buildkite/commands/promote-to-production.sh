#!/usr/bin/env bash

set -eu

# Releases the build gathered by the preceding step to the production track, once a developer has
# confirmed the block step. The version code is passed as the first argument; the Yes/No
# confirmation is read from Buildkite meta-data. No build — just gems + secrets.

VERSION_CODE="${1:-}"

# `production_ready_to_release` must stay in sync with PRODUCTION_CONFIRM_META_DATA_KEY in
# fastlane/lanes/promote.rb, which is the key the block-step confirmation field writes.
READY="$(buildkite-agent meta-data get "production_ready_to_release" --default "no")"

if [[ "${READY}" != "yes" ]]; then
  echo "--- :no_entry_sign: Release not confirmed (production_ready_to_release=${READY}); skipping."
  exit 0
fi

if [[ -z "${VERSION_CODE}" ]]; then
  echo "+++ :x: No version code was provided to release."
  exit 1
fi

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :rocket: Releasing ${VERSION_CODE} to the production track"
bundle exec fastlane promote_to_production version_code:"${VERSION_CODE}"
