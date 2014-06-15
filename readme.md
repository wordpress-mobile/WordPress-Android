# WordPress for Android #

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

WordPress for Android uses open sources libraries, some of them  (email-
checker and android-passcodelock for instance) are developed and tested within
the WordPress for Android project. To ease the development process and to use
them in other projects, we use separate github projects and git repositories
and we added them as local dependencies in our gradle configuration.

After you cloned the main repository, you'll have to update pull these
libraries which are git submodules:

    $ git submodule init
    $ git submodule update

The [gradle build system][2] will fetch all other dependencies and generate
files you need to build the project. You only need to generate the
local.properties file and create the gradle.properties file, the easiest
way is to copy our example:

    $ android update project -p . --target android-14 # generate or update local.properties
    $ cp gradle.properties-example gradle.properties

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
