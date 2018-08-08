Screenshot Device Setup
================
There are two main things you need to know to create or edit screenshot devices. 

- Emulators require a particular virtual device hardware profile, which is an `xml` file with details on the virtualized hardware. They reference this hardware profile by its device name and manufacturer. Rather than use defaults that ship with Android Studio (and may change over time), we've definied new hardware profiles specifically for our own use.
- Emulators can be entirely defined within their `.ini` file. Copying the `.ini` file into the correct directory (along with _another_ `.ini` file to point to it) is all that's needed – if it exists, the Android SDK will happily build a fresh new virtual device based on it.

#### Making Changes

To adjust details of the particular virtual device hardware profile (say the screen size), it's usually just easiest to create a new device using the Android Studio GUI (it'll handle things like the screen size and DPI calculations for you). Right-clicking on a hardware profile will allow you to export it to an `xml` file. You'll likely want to hand-edit the `xml` file to make sure it has the correct name and manufacturer set. Then you can overwrite the file in the repository with your new one.

After making changes to either the hardware profile or emulator properties, just run `bundle exec fastlane android rebuild_screenshot_devices` to completely rebuild the devices based on your new settings. It'll only rebuild the hardware profiles and emulators definied in the repository, so if you have others already defined, they'll be left alone.
