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

function checkSamsungWorkaround() {
	/bin/echo -n "Check for the Samsung android.support.v7.view.menu workaround..."
	apktool > /dev/null 2>&1 || (pFail; echo "You need apktool installed to run this check (brew install apktool)"; exit 1) || exit 4
	./gradlew clean --offline > /dev/null 2>&1
	./gradlew assembleVanillaRelease --offline > /dev/null 2>&1
	rm -rf /tmp/wpandroid-checksamsungworkaround/
	apktool -f -r d WordPress/build/outputs/apk/WordPress-vanilla-release-unaligned.apk -o /tmp/wpandroid-checksamsungworkaround/ > /dev/null && ls -1 /tmp/wpandroid-checksamsungworkaround/smali/android/support/v7/view/menu/MenuBuilder* > /dev/null 2>&1
	if [ $? -eq 0 ]; then
		pFail
		echo "See http://stackoverflow.com/q/24809580/58332 for more informations"
		exit 4
	fi
	pOk
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
	langs=`curl -L http://translate.wordpress.org/projects/apps/android/dev 2> /dev/null \
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

function printVersion() {
	gradle_version=$(grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+" )
	echo "$BUILDFILE version $gradle_version"
}

function checkGradleProperties() {
	/bin/echo -n "Check WordPress/gradle.properties..."
	checksum=`cat WordPress/gradle.properties | grep -v "^wp.debug." | grep "^wp."|tr "[A-Z]" "[a-z]" | sed "s/ //g" | sort | sha1sum | cut -d- -f1 | sed "s/ //g"`
	known_checksum="4058cdf3d784e4b79f63514d4780e92c28b5ab78"
	if [ x$checksum != x$known_checksum ]; then
		pFail
		exit 5
	fi
	pOk
}

checkNewLanguages
checkENStrings
checkSamsungWorkaround
checkGradleProperties
printVersion
# checkDeviceToTest
# runConnectedTests
