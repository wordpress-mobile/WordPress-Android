#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :globe_with_meridians: Downloading translations and updating the PR"
bundle exec fastlane update_translations
