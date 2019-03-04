Screenshot Device Setup
================
Emulators can be entirely defined within their `.ini` file. Copying the `.ini` file into the correct directory (along with _another_ `.ini` file to point to it) is all that's needed – if it exists, the Android SDK will happily build a fresh new virtual device based on it.

##### To adjust the emulator

It's typically easiest to create a new emulator from scratch inside of Android Studio. Right-clicking on an emulator, then choosing "Show on Disk" will bring you to the emulator folder. Copy the `config.ini` file to the emulators directory, overwriting the configuration you wish to make changes to.  When merging, it can be useful to use the diff view to ensure that only the fields you meant to change are actually being changed – as a for-instance, the `AvdId` that Android Studio generates is usually incorrect, and modifying that key will break the screenshot generation scripts.
