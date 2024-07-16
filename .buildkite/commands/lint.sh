#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :globe_with_meridians: Localization check"
bundle exec fastlane check_locales_consistency app:$1

echo "--- :microscope: Linting"
cp gradle.properties-example gradle.properties

if [ "$1" = "wordpress" ]; then
	./gradlew lintWordpressVanillaRelease
	exit 0
fi

if [ "$1" = "jetpack" ]; then
	./gradlew lintJetpackVanillaRelease
	exit 0
fi

echo "No target provided – unable to lint"
exit 1
