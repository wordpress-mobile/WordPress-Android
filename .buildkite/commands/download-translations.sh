#!/bin/bash -eu

echo "--- :robot_face: Use bot for git operations"
source use-bot-for-git

echo "--- :ruby: Setup Ruby Tools"
install_gems

echo "--- :globe_with_meridians: Download translations and open/update the PR"
bundle exec fastlane update_translations
