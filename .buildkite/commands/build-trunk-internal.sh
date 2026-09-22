#!/usr/bin/env bash

set -eu

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :hammer_and_wrench: Build WordPress & Jetpack and upload to the Play internal track"
bundle exec fastlane build_and_upload_trunk_internal skip_confirm:true
