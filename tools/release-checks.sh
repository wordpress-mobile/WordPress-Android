#!/bin/sh

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
		/bin/echo -n "Unstagged changes detected in res/ - can't continue..."
		pFail
		exit 3
	fi
	# save local changes
	git stash | grep "No local changes to save" > /dev/null
	needpop=$?

	rm -f res/values-??/strings.xml
	/bin/echo -n "Check for missing strings (slow)..."
	./gradlew build > /dev/null 2>&1 && pOk || (pFail; ./gradlew build)
	git checkout -- res/

	# restore local changes
	if [ $needpop -eq 1 ]; then
	     git stash pop > /dev/null
	fi
}

function checkVersions() {
	gradle_version=$(grep -E 'versionName' build.gradle \
		             | grep -Eo "[0-9.]+")
	tag=$(git tag -l|sort|tail -1)
	if [[ $gradle_version != $tag ]]; then
		/bin/echo -n "build.gradle version and git tag version mismatch..."
		pFail
	fi
	echo "build.gradle version $gradle_version"
	echo "last git tag version is $tag"
}


# Check strings
checkENStrings

# Run tests
# checkDeviceToTest
# runConnectedTests

checkVersions
