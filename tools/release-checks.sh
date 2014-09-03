#!/bin/sh

LANG_FILE=tools/exported-language-codes.csv
RESDIR=WordPress/src/main/res/
BUILDFILE=WordPress/build.gradle

function checkDeviceToTest() {
	lines=$(adb devices -l|wc -l)
	if [ $lines -le 2 ]; then
		echo You need a device connected or an emulator running
		exit 2
	fi
}

function runConnectedTests() {
	echo Tests will be run on following devices:
	adb devices -l
	echo -----------
	./gradlew cIT
}

function pOk() {
	echo "[$(tput setaf 2)OK$(tput sgr0)]"
}

function pFail() {
	echo "[$(tput setaf 1)KO$(tput sgr0)]"
}

function checkENStrings() {
	if [[ -n $(git status --porcelain|grep "M res") ]]; then
		/bin/echo -n "Unstagged changes detected in $RESDIR/ - can't continue..."
		pFail
		exit 3
	fi
	# save local changes
	git stash | grep "No local changes to save" > /dev/null
	needpop=$?

	rm -f $RESDIR/values-??/strings.xml $RESDIR/values-??-r??/strings.xml
	/bin/echo -n "Check for missing strings (slow)..."
	./gradlew build > /dev/null 2>&1 && pOk || (pFail; ./gradlew build)
	./gradlew clean > /dev/null 2>&1
	git checkout -- $RESDIR/

	# restore local changes
	if [ $needpop -eq 1 ]; then
	     git stash pop > /dev/null
	fi
}

function checkNewLanguages() {
	/bin/echo -n "Check for potential new languages..."
	langs=`curl http://translate.wordpress.org/projects/android/dev 2> /dev/null \
   		| grep -B 1 morethan90|grep "android/dev/" \
   		| sed "s+.*android/dev/\([a-zA-Z-]*\)/default.*+\1+"`
	nerrors=''
	for lang in $langs; do
		grep "^$lang," $LANG_FILE > /dev/null
		if [ $? -ne 0 ]; then
			nerrors=$nerrors"language code $lang has reached 90% translation threshold and hasn't been found in $LANG_FILE\n"
		fi
	done
	if [ "x$nerrors" = x ]; then
		pOk
	else
		pFail
		echo $nerrors
	fi
}

function checkVersions() {
	gradle_version=$(grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+" )
	tag=$(git for-each-ref --sort=taggerdate --format '%(tag)' refs/tags|tail -1)
	if [[ $gradle_version != $tag ]]; then
		/bin/echo -n "$BUILDFILE version and git tag version mismatch..."
		pFail
	fi
	echo "$BUILDFILE version $gradle_version"
	echo "last git tag version is $tag"
}

checkNewLanguages
checkENStrings
checkVersions
# checkDeviceToTest
# runConnectedTests
