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

function checkENStrings() {
	# save local changes
	git stash | grep "No local changes to save" > /dev/null
	needpop=$?

	rm -f res/values-??/strings.xml
	./gradlew build
	git checkout -- res/

	# restore local changes
	if [ $needpop -eq 1 ]; then
	    git stash pop > /dev/null
	fi
}

# Check strings
checkENStrings

# Run tests
# checkDeviceToTest
# runConnectedTests