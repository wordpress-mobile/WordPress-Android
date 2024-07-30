#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :clipboard: Copying gradle.properties"
cp gradle.properties-example gradle.properties

echo "--- :globe_with_meridians: Check Locales Declaration Consistency"
bundle exec fastlane check_declared_locales_consistency app:"$1"

echo "--- :microscope: Linting"

if [ "$1" = "wordpress" ]; then
	./gradlew lintWordpressVanillaRelease

  gzip -c 'WordPress/build/reports/lint-results-wordPressVanillaRelease.sarif' | base64 > sarif_base64.tmp

  jq -n \
   --arg commit_sha "$BUILDKITE_COMMIT" \
   --arg pr_number "$BUILDKITE_PULL_REQUEST" \
   --rawfile sarif sarif_base64.tmp \
 '{
   "commit_sha": $commit_sha,
   "ref": ("refs/pull/"+$pr_number+"/head"),
   "sarif": $sarif
 }'

  rm sarif_base64.tmp
	exit 0
fi

if [ "$1" = "jetpack" ]; then
	./gradlew lintJetpackVanillaRelease
	exit 0
fi

echo "No target provided – unable to lint"
exit 1
