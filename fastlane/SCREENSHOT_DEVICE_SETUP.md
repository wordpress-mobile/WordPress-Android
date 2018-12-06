Screenshot Device Setup
================
There are two main things you need to know to create or edit screenshot devices. 

- Emulators require a particular virtual device hardware profile, which is an `xml` file with details on the virtualized hardware. They reference this hardware profile by its device name and manufacturer. Rather than use defaults that ship with Android Studio (and may change over time), we've definied new hardware profiles specifically for our own use.
- Emulators can be entirely defined within their `.ini` file. Copying the `.ini` file into the correct directory (along with _another_ `.ini` file to point to it) is all that's needed – if it exists, the Android SDK will happily build a fresh new virtual device based on it.

#### Making Changes

To adjust details of the particular virtual device hardware profile (say the screen size), it's usually just easiest to create a new device using the Android Studio GUI (it'll handle things like the screen size and DPI calculations for you).

After making changes to either the hardware profile or emulator properties, just run `bundle exec fastlane android rebuild_screenshot_devices` to completely rebuild the devices based on your new settings. It'll only rebuild the hardware profiles and emulators definied in the repository, so if you have others already defined, they'll be left alone.

##### To adjust device parameters:

Edit the device in Android Studio, and adjust to the specifications you'd like to use. Once you save the device, it'll make a new hardware profile with the settings you just provided. Right-click on that hardware profile, and choose "export". When merging, it can be useful to use the diff view to ensure that only the fields you meant to change are actually being changed – as a for-instance, Android Studio loves to try to overwrite your `sdcard.size`, and that change can almost always be discarded.

##### To adjust the emulator

It's typically easiest to create a new emulator from scratch inside of Android Studio. Right-clicking on an emulator, then choosing "Show on Disk" will bring you to the emulator folder. Copy the `config.ini` file to the emulators directory, overwriting the configuration you wish to make changes to.  When merging, it can be useful to use the diff view to ensure that only the fields you meant to change are actually being changed – as a for-instance, the `AvdId` that Android Studio generates is usually incorrect, and modifying that key will break the screenshot generation scripts.

#### Troubleshooting

##### When I make changes to the emulator, it says "Repair Device" in Android Studio _or_ when I run the emulator, it's not using the screen size I provided.

You have likely changed the hardware profile without rebuilding the emulator, so the `hw.device.hash2` field in the `${devicename}.ini` file is incorrect. Rebuild the device from the hardware profile in Android Studio, then save your changes back into the repository.
