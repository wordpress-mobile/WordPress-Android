#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🛠 Download Mobile App Dependencies [Assemble Jetpack App]"
./gradlew assembleJetpackJalapenoDebug

echo "--- 🧹 Download Lint Dependencies [Lint Jetpack App]"
./gradlew lintJetpackJalapenoDebug

echo "--- 🧹 Download Detekt Dependencies [Run Detekt]"
./gradlew detekt

echo "--- 🧹 Download Checkstyle Dependencies [Run Checkstyle]"
./gradlew checkstyle

echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew assembleJetpackJalapenoDebugUnitTest libs:processors:testClasses libs:image-editor:assembleDebugUnitTest libs:fluxc:assembleDebugUnitTest libs:login:assembleDebugUnitTest

echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleJetpackJalapenoDebugAndroidTest

save_gradle_dependency_cache
