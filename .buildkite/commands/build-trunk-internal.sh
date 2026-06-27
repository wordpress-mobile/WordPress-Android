#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Build WordPress & Jetpack and upload to the Play internal track"
bundle exec fastlane build_and_upload_trunk_internal skip_confirm:true skip_prechecks:true
