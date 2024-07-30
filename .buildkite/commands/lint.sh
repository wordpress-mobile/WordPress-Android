#!/bin/bash -eu

pwd

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :clipboard: Copying gradle.properties"
cp gradle.properties-example gradle.properties

echo "--- :globe_with_meridians: Check Locales Declaration Consistency"
bundle exec fastlane check_declared_locales_consistency app:"$1"

echo "--- :microscope: Linting"

if [ "$1" = "wordpress" ]; then
	./gradlew lintWordpressVanillaRelease

  gzip -c "WordPress/build/reports/lint-results-wordpressVanillaRelease.sarif" | base64 > sarif_base64.tmp

  ls "WordPress/build/reports/"

  json=$(jq -n \
   --arg commit_sha "$BUILDKITE_COMMIT" \
   --arg pr_number "$BUILDKITE_PULL_REQUEST" \
   --rawfile sarif sarif_base64.tmp \
 '{
   "commit_sha": $commit_sha,
   "ref": ("refs/pull/"+$pr_number+"/head"),
   "sarif": $sarif
 }')

 echo "$json" > payload.json

 curl -L \
   -X POST \
   -H "Accept: application/vnd.github+json" \
   -H "Authorization: Bearer $GITHUB_TOKEN" \
   -H "X-GitHub-Api-Version: 2022-11-28" \
   --data-binary "@payload.json" \
    https://api.github.com/repos/wordpress-mobile/WordPress-Android/code-scanning/sarifs

  rm sarif_base64.tmp
	exit 0
fi

if [ "$1" = "jetpack" ]; then
	./gradlew lintJetpackVanillaRelease
	exit 0
fi

echo "No target provided – unable to lint"
exit 1
