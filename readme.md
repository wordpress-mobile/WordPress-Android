# WordPress for Android #

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

The [gradle build system][2] will fetch all dependencies and generate
files you need to build the project. You first need to copy create the
local.properties file, the easiest way is to copy our example:

    $ cp local.properties-example local.properties

Then edit the local.properties file to add your Android
`sdk.dir`. After this step, you can invoke gradle to build, install
and test the project:

    $ ./gradlew assembleDebug # assemble the debug .apk
    $ ./gradlew installDebug  # assemble and install the debug .apk if you
                              # have an emulator or an Android device connected
    $ ./gradlew cIT           # assemble, install and run unit tests

Note: you can use the [Android Studio IDE][3], import the project as a
Gradle project.

## Need help to build or hack? ##

Say hello on our IRC channel: `#WordPress-Android` (freenode). Read our
[Developer Handbook][4] and [Development Blog][5].

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[2]: http://tools.android.com/tech-docs/new-build-system/user-guide
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: http://make.wordpress.org/mobile/handbook/
[5]: http://make.wordpress.org/mobile/
