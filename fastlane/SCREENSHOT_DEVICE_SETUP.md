Screenshot Device Setup
================

##### What is this?
The `rebuild_screenshot_devices` lane allows you to recreate the devices used for screenshots on demand.

##### Why do we have it?
It helps ensure that everyone is using exactly the same devices for screenshot generation. Additionally, it makes it possible to provision the devices from the command line in CI. Lastly, if the emulator is acting flaky when developing, it makes it easy to destroy and recreate it.


##### How does it work?
Emulators live in `~/.android/avd`.  When they're first created, they're just a `.ini`  file – on first run, the Android SDK turns it into a virtual machine. This makes it possible to script their creation.

Copying the `.ini` file into the correct directory (along with _another_ `.ini` file to point to it) is all that's needed – if it exists, the Android SDK will happily build a fresh new virtual device based on it.

##### To adjust the emulator

It's typically easiest to create a new emulator from scratch inside of Android Studio. Right-clicking on an emulator, then choosing "Show on Disk" will bring you to the emulator folder. Copy the `config.ini` file to the emulators directory, overwriting the configuration you wish to make changes to.  When merging, it can be useful to use the diff view to ensure that only the fields you meant to change are actually being changed – as a for-instance, the `AvdId` that Android Studio generates is usually incorrect, and modifying that key may break the screenshot generation scripts.
