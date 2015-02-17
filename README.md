# WordPress for Android #

If you're just looking to install WordPress for Android, you can find
it on [Google Play][1]. If you're a developer wanting to contribute,
read on.

## Build Instructions ##

The [gradle build system][2] will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR with your actual android SDK directory)
file and create the gradle.properties file. The easiest way is to copy
our example:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ cp ./WordPress/gradle.properties-example ./WordPress/gradle.properties

Note: this is the default `./WordPress/gradle.properties` file. If you
want to use WordPress.com functions (login to a WordPress.com account,
access the Reader and Stats for example), you'll have to get a WordPress.com
OAuth2 ID and secret. Please read the
[OAuth2 Authentication](#oauth2-authentication) section.

The previous command creates a `libs/` directory and clones all dependencies needed
by the main WordPress for Android project. You can now build, install and
test the project:

    $ ./gradlew assembleVanillaDebug # assemble the debug .apk
    $ ./gradlew installVanillaDebug  # install the debug .apk if you have an
                                     # emulator or an Android device connected
    $ ./gradlew WordPress:cAT        # assemble, install and run unit tests

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

## License

WordPress for Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[2]: http://tools.android.com/tech-docs/new-build-system/user-guide
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: https://make.wordpress.org/chat/
[5]: https://developer.wordpress.com/apps/
[6]: https://developer.wordpress.com/docs/oauth2/
[7]: https://developer.wordpress.com/docs/api/
