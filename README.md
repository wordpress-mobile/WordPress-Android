# WordPress for Android #

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-Android)

If you're just looking to install WordPress for Android, you can find
it on [Google Play](https://play.google.com/store/apps/details?id=org.wordpress.android). If you're a developer wanting to contribute, read on.


## Build Instructions ##

1. Make sure you've installed [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Android Studio](https://developer.android.com/studio/index.html), a _Standard Setup_ would work.
2. Clone this GitHub repository.
3. Copy `gradle.properties-example` to `gradle.properties`.
4. In Android Studio open the project from the local repository as a **Gradle project** (this will auto-generate `local.properties` with the SDK location).
5. Make sure you have an emulation device setup in AVD Manager (_Tools → Android → AVD Manager_).
6. Run.

Notes:

* To use WordPress.com features (login to WordPress.com, access Reader and Stats, etc) you need a WordPress.com OAuth2 ID and secret. Please read the [OAuth2 Authentication](#oauth2-authentication) section.

Once installed, you can now build, install and test the project from the command line:

    $ ./gradlew assembleVanillaDebug                        # assemble the debug .apk
    $ ./gradlew installVanillaDebug                         # install the debug .apk if you have an
                                                            # emulator or an Android device connected
    $ ./gradlew :WordPress:testVanillaDebugUnitTest         # assemble, install and run unit tests
    $ ./gradlew :WordPress:connectedVanillaDebugAndroidTest # assemble, install and run Android tests


## Directory structure ##                
    .
    ├── libs                    # dependencies used to build debug variants
    ├── tools                   # script collection
    ├── gradle.properties       # properties imported by the build script
    ├── WordPress
    │   |-- build.gradle        # main build script
    │   └── src
    │       ├── androidTest     # Android test assets, resources and code
    │       ├── test            # Unit tests
    │       ├── main
    │       │   ├── assets      # main project assets
    │       │   ├── java        # main project java code
    │       │   └── res         # main project resources
    │       ├── debug           # debug variant
    │       └── wasabi          # wasabi variant specific resources and manifest

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
need to edit the `./gradle.properties` file and change the
`WP.OAUTH.APP_ID` and `WP.OAUTH.APP_SECRET` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account. Note that authenticating to WordPress.com via Google is not supported in development builds of the app, only in the official release.

Note that credentials created with our [WordPress.com applications manager][5] allow login only and not signup. New
accounts must be created using the [official app][1] or [on the web](https://wordpress.com/start). Login is restricted
to the WordPress.com account with which the credentials were created. Also, you will be able to interact with sites of
that same WordPress.com account only. In other words, if the credentials were created with foo@email.com, you will only
be able to login with foo@email.com and access foo@email.com sites. Using another account like bar@email.com will cause
the `Client cannot use "password" grant_type` error. 

Read more about [OAuth2][6] and the [WordPress.com REST endpoint][7].

## Google Configuration ##

Google Sign-In is only available for WordPress.com accounts through the [official app][1].
Contributors can build and run the app without issue, but Google Sign-In will always fail.
Google Sign-In requires configuration files which contain client and server information
that can't be shared publicly. More documentation and guides can be found on the
[Google Identity Platform website][8].

## How we work ##

You can read more about [Code Style Guidelines](CODESTYLE.md) we adopted, and
how we're organizing branches in our repository in the
[Contribution Guide](CONTRIBUTING.md).

## Need help to build or hack? ##

Say hello on our [Slack][4] channel: `#mobile`.

## FAQ ##

* Q: I can't build/test/package the project because of a `PermGen space` error.
* A: Create a `gradle.properties` file in the project root directory with the
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
[8]: https://developers.google.com/identity/
