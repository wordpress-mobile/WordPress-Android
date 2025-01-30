#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :globe_with_meridians: Check Locales Declaration Consistency"
bundle exec fastlane check_declared_locales_consistency app:"$1"

echo "--- :microscope: Linting"

if [ "$1" = "wordpress" ]; then
  ./gradlew lintWordpressVanillaRelease
  exit 0
fi

if [ "$1" = "jetpack" ]; then
  set +e
  ./gradlew lintJetpackVanillaRelease
  lint_exit_code=$?
  set -e

  upload_sarif_to_github "WordPress/build/reports/lint-results-jetpackVanillaRelease.sarif"
  exit $lint_exit_code
fi

echo "No target provided – unable to lint"
exit 1
