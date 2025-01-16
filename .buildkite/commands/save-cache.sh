#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

# .buildkite/commands/prototype-build.sh -> build_and_upload_$1_prototype_build
# -> PROTOTYPE_BUILD_FLAVOR = 'Jalapeno'
# -> PROTOTYPE_BUILD_TYPE = 'Debug'
echo "--- 🛠 Download Mobile App Dependencies [Assemble Jetpack App]"
./gradlew assembleJetpackJalapenoDebug
echo ""

# .buildkite/commands/lint.sh -> ./gradlew lintJetpackVanillaRelease
echo "--- 🧹 Download Lint Dependencies [Lint Jetpack App]"
./gradlew lintJetpackJalapenoDebug
echo ""

# .buildkite/commands/run-unit-tests.sh
# -> test_suite="testWordpressVanillaRelease koverXmlReportWordpressVanillaRelease"
# -> test_suite=":libs:processors:test :libs:processors:koverXmlReport"
# -> test_suite=":libs:image-editor:testReleaseUnitTest :libs:image-editor:koverXmlReportRelease"
# -> test_suite=":libs:fluxc:testReleaseUnitTest :libs:fluxc:koverXmlReportRelease"
# -> test_suite=":libs:login:testReleaseUnitTest :libs:login:koverXmlReportRelease"
echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew assembleJetpackJalapenoDebugUnitTest assembleDebugUnitTest testClasses
echo ""

# .buildkite/commands/run-instrumented-tests.sh -> build_and_run_instrumented_test
# -> gradle(tasks: ["WordPress:assemble#{app.to_s.capitalize}VanillaDebug", "WordPress:assemble#{app.to_s.capitalize}VanillaDebugAndroidTest"])
echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleJetpackJalapenoDebugAndroidTest
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
