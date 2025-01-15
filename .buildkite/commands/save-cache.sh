#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🛠 Download Mobile App Dependencies [Assemble Jetpack App]"
./gradlew assembleJetpackJalapenoDebug
echo ""

echo "--- 🧹 Download Lint Dependencies [Lint Jetpack App]"
./gradlew lintJetpackJalapenoDebug
echo ""

echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew assembleJetpackJalapenoDebugUnitTest assembleDebugUnitTest testClasses
echo ""

echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleJetpackJalapenoDebugAndroidTest
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
