# Tool `playstore-screenshots.py`

This tool helps you take screenshots for the Play Store. It uses the [AndroidViewClient](https://github.com/dtmilano/AndroidViewClient) python library to control and send commands to emulators or devices. It connects to a real blog and change the emulator or device language to take localized screenshots.

## Prerequisites

* your devices or emulators must be connected to the Internet and rooted.
* `adb` binary must be in your `PATH` when you run the tool.

## Setup

Install dependencies: Python 2.x and easy_install, then you have to install AndroidViewClient

	$ easy_install --upgrade androidviewclient

Then edit the `settings.py` file. Copy the example file and edit it. Change the username / password, languages and sample text.

	$ cp settings.py-example settings.py

## Run

	$ ./playstore-screenshots.py PACKAGE_NAME APK_FILE
	$ # Example: ./playstore-screenshots.py org.wordpress.android WordPress-vanilla-release.apk
	$ # Example: ./playstore-screenshots.py org.wordpress.android.beta WordPress-zbetagroup-release.apk

## Example usage

1. Unplug real devices from your computer
1. Start 3 Genymotion emulators: a Nexus 5 emulated screen, a Nexus 7 emulated screen and a Nexus 9 emulated screen.
1. Set the Nexus 7 and 9 in landscape mode.
1. Run the script:

	    ./playstore-screenshots.py org.wordpress.android ../../../../WordPress/build/outputs/apk/WordPress-vanilla-release.apk

1. You'll find the screenshot files (eg. `fr-drawer-opened-Google_Nexus_5___5_0_0___API_21___1080x1920.png`) in the same directory.

