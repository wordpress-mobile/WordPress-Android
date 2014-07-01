# WordPress for Android #

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

The [gradle build system][2] will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties file and create the gradle.properties file, the easiest
way is to copy our example:

    $ android update project -p . --target android-14 # generate or update local.properties
    $ cp ./WordPress/gradle.properties-example ./WordPress/gradle.properties
    
Checkout our library projects to create a debug build:
  - checkout the project [WordPress-Utils-Android][6]
  - create a simbolic link to the WordPress-Utils-Android project by calling 
  - `ln -s ../WordPress-Utils-Android/WordPressUtils/ WordPressUtils` from the main folder of this project

Note: generated `build.xml`, `proguard-project.txt` and `project.properties` are not used and can be deleted.
After this step, you can invoke gradle to build, install and test the project:

    $ ./gradlew assembleDebug # assemble the debug .apk
    $ ./gradlew installDebug  # assemble and install the debug .apk if you
                              # have an emulator or an Android device connected
    $ ./gradlew cAT           # assemble, install and run unit tests

Note: you can use the [Android Studio IDE][3], import the project as a
Gradle project.

## Need help to build or hack? ##

Say hello on our IRC channel: `#WordPress-Mobile` (freenode). Read our
[Developer Handbook][4] and [Development Blog][5].

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[2]: http://tools.android.com/tech-docs/new-build-system/user-guide
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: http://make.wordpress.org/mobile/handbook/
[5]: http://make.wordpress.org/mobile/
[6]: https://github.com/wordpress-mobile/WordPress-Utils-Android
