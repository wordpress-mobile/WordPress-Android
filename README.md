# WordPress for Android #

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-Android)

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

You first need to generate the `local.properties` (replace YOUR_SDK_DIR with
your actual android SDK directory) file and create the `gradle.properties` file:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ cp ./WordPress/gradle.properties-example ./WordPress/gradle.properties

Note: this is the default `./WordPress/gradle.properties` file. If you
want to use WordPress.com features (login to a WordPress.com account,
access the Reader and Stats for example), you'll have to get a WordPress.com
OAuth2 ID and secret. Please read the
[OAuth2 Authentication](#oauth2-authentication) section.

You can now build, install and test the project:

    $ ./gradlew assembleVanillaDebug # assemble the debug .apk
    $ ./gradlew installVanillaDebug  # install the debug .apk if you have an
                                     # emulator or an Android device connected
    $ ./gradlew cAT                  # assemble, install and run unit tests

You can use [Android Studio][3] by importing the project as a Gradle project.

### Additional Build Instructions for Windows ###

The [visual editor][10] uses a linux-style symlink for its [assets folder][11],
which has to be converted to a Windows symlink.

From git bash, inside the project root:

    $ rm libs/editor/WordPressEditor/src/main/assets
    $ git ls-files --deleted -z | git update-index --assume-unchanged -z --stdin

Then, from a Windows command prompt:

    mklink /D [PROJECT_ROOT]\libs\editor\WordPressEditor\src\main\assets %PROJECT_ROOT%\libs\editor\libs\editor-common\assets

Finally, update `[PROJECT_ROOT]\.git\info\exclude` to ignore the symlink
locally:

    # editor assets symlink
    libs/editor/WordPressEditor/src/main/assets

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
            `-- wasabi          # wasabi variant specific resources and manifest

## OAuth2 Authentication ##

In order to use WordPress.com functions you will need a client ID and
a client secret key. These details will be used to authenticate your
application and verify that the API calls being made are valid. You can
create an application or view details for your existing applications with
our [WordPress.com applications manager][5].

When creating your application, you should select "Native client" for the
application type. The applications manager currently requires a "redirect URL",
but this isn't used for mobile apps. Just use "https://localhost".

Once you've created your application in the [applications manager][5], you'll
need to edit the `./WordPress/gradle.properties` file and change the
`WP.OAUTH.APP.ID` and `WP.OAUTH.APP.SECRET` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account.

Read more about [OAuth2][6] and the [WordPress.com REST endpoint][7].

## How we work ##

You can read more about [Code Style Guidelines](CODESTYLE.md) we adopted, and
how we're organizing branches in our repository in the
[Contribution Guide](CONTRIBUTING.md).

## Need help to build or hack? ##

Say hello on our [Slack][4] channel: `#mobile`.

## Alternative Build Instructions ##

WordPress-Android can be compiled with [Buck][8], an alternative to Gradle,
that makes the build process much faster. You first need to fetch the
dependencies by doing:

    $ buck fetch wpandroid

This command will fetch all dependencies (`.aar` and `.jar`) needed to build
the project. Then you can run buck to build the apk:

    $ buck build wpandroid

You can build, install and run the project if you have a device or an emulator
connected by running the following:

    $ buck install --run wpandroid

It's recommended to install [watchman][9] to take advantage of the Buck
daemon: `buckd`.

## FAQ ##

* Q: I can't build/test/package the project because of a `PermGen space` error.
* A: Create a `build.properties` file in the project root directory with the
following: `org.gradle.jvmargs=-XX:MaxPermSize=1024m`.

## License ##

WordPress for Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: https://make.wordpress.org/chat/
[5]: https://developer.wordpress.com/apps/
[6]: https://developer.wordpress.com/docs/oauth2/
[7]: https://developer.wordpress.com/docs/api/
[8]: https://facebook.github.io/buck
[9]: https://facebook.github.io/watchman/docs/install.html
[10]: https://github.com/wordpress-mobile/WordPress-Editor-Android
[11]: https://github.com/wordpress-mobile/WordPress-Android/blob/develop/libs/editor/WordPressEditor/src/main/assets
