#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
#if [ "$1" = "wordpress" ]; then
#  ./gradlew assembleWordpressWasabiDebug -Dorg.gradle.caching.debug=true --rerun-tasks --console=plain
#fi
#
#if [ "$1" = "jetpack" ]; then
#  ./gradlew assembleJetpackWasabiDebug -Dorg.gradle.caching.debug=true
#fi

./gradlew :WordPress:compileWordpressWasabiDebugJavaWithJavac -Dorg.gradle.caching.debug=true --rerun --console=plain

#find libs/ -type f -name "annotations.jar" -exec cp {} . \;
#find libs/ -type f -name "fluxc-annotations.jar" -exec cp {} . \;
find ~.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.1.10/ -type f -name "kotlin-stdlib-2.1.10.jar" -exec cp {} . \;
