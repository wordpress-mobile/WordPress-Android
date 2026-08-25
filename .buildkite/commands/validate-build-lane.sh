#!/usr/bin/env bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Running build_and_upload_trunk_internal with prechecks ON (stop before Play Store upload)"
LOG="$(mktemp)"
# Deliberately no `skip_prechecks:true`: the prechecks block is the code this PR changed.
bundle exec fastlane build_and_upload_trunk_internal skip_confirm:true | tee "${LOG}"

if ! grep -F 'VALIDATION: prechecks block completed' "${LOG}"; then
  echo "VALIDATION: lane did not print the prechecks marker; refusing to treat this as a pass"
  exit 1
fi

if ! grep -F 'VALIDATION: bundle built' "${LOG}"; then
  echo "VALIDATION: lane did not print the bundle marker; refusing to treat this as a pass"
  exit 1
fi
