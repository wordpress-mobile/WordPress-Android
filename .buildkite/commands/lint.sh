#!/bin/bash -eu

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
