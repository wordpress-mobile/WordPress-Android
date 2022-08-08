#!/bin/bash -eu

echo "--- DEBUG: Skipping step for CI debugging purposes"
exit 0

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
