# WordPress for Android #

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

The [gradle build system][2] will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR by your actual android sdk dir)
file and create the gradle.properties file, the easiest way is to copy
our example:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ cp ./WordPress/gradle.properties-example ./WordPress/gradle.properties

Previous command create a `libs/` directory and clone all dependencies needed
by the main WordPress for Android project. You can now build, install and
test the project:

    $ ./gradlew assembleVanillaDebug # assemble the debug .apk
    $ ./gradlew installVanillaDebug  # install the debug .apk if you have an
                                     # emulator or an Android device connected
    $ ./gradlew cAT                  # assemble, install and run unit tests

You can use [Android Studio][3] by importing the project as a Gradle project.

## Directory structure ##

    |-- libs                    # dependencies used to build debug variants
    |-- tools                   # script collection
    `-- WordPress
        |-- build.gradle        # main build script
        |-- gradle.properties   # properties imported by the build script
        `-- src                 # android specific Java code
            |-- androidTest     # test assets, resources and code
            |-- main            #
            |   |-- assets      # main project assets
            |   |-- java        # main project java code
            |   `-- res         # main project resources
            |-- vanilla         # vanilla variant specific manifest
            `-- zbetagroup      # beta variant specific resources and manifest

## Need help to build or hack? ##

Say hello on our IRC channel: `#WordPress-Mobile` (freenode). Read our
[Developer Handbook][4] and [Development Blog][5].

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[2]: http://tools.android.com/tech-docs/new-build-system/user-guide
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: http://make.wordpress.org/mobile/handbook/
[5]: http://make.wordpress.org/mobile/
