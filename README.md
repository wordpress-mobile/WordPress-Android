# WordPress for Android #

If you're just looking to install WordPress for Android, you can find it on [Google Play](https://play.google.com/store/apps/details?id=org.wordpress.android&referrer=utm_source%3Dgithub%26utm_medium%3Dwebsite).
If you're a developer wanting to contribute, read on.

## Build Instructions ##

1. Make sure you've installed [Android Studio](https://developer.android.com/studio).
1. Install npm using [Node Version Manager](https://github.com/nvm-sh/nvm)(nvm), as described in step one from the [Block Editor Quickstart guide](https://developer.wordpress.org/block-editor/getting-started/devenv/#quickstart)
1. `cd WordPress-Android` to enter the working directory.
1. `cp gradle.properties-example gradle.properties` to set up the sample app credentials file.
1. In Android Studio, open the project from the local repository. This will auto-generate `local.properties` with the SDK location.
1. Recommended: The CI uses JDK11 to build the app and run the tests. Some tests won't pass on the JDK embedded in Android Studio (JDK8). You might want to set JAVA_HOME and JDK location in Android Studio to JDK11.
1. Go to Tools → AVD Manager and create an emulated device.
1. Run.

Notes:

* While loading/building the app in Android Studio, ignore the prompt to update the Gradle plugin version, as that will probably introduce build errors. On the other hand, feel free to update if you are planning to work on ensuring the compatibility of the newer version.

## Build and Test ##

To build, install, and test the project from the command line:

    $ ./gradlew assembleWordPressVanillaDebug                        # assemble the debug .apk
    $ ./gradlew installWordPressVanillaDebug                         # install the debug .apk if you have an
                                                                     # emulator or an Android device connected
    $ ./gradlew :WordPress:testWordPressVanillaDebugUnitTest         # assemble, install and run unit tests
    $ ./gradlew :WordPress:connectedWordPressVanillaDebugAndroidTest # assemble, install and run Android tests

## Running the app ##

You can use your own WordPress site for developing and testing the app. If you don't have one, you can create a temporary test site for free at https://jurassic.ninja/.
On the app start up screen, choose "Enter your existing site address" and enter the URL of your site and your credentials.

Note: Access to WordPress.com features is temporarily disabled in the development environment.

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

## Google Configuration ##

Google Sign-In is only available for WordPress.com accounts through the [official app][1].
Contributors can build and run the app without issue, but Google Sign-In will always fail.
Google Sign-In requires configuration files which contain client and server information
that can't be shared publicly. More documentation and guides can be found on the
[Google Identity Platform website][8].

## Contributing

Read our [Contributing Guide](CONTRIBUTING.md) to learn about reporting issues, contributing code, and more ways to contribute.

## Security

If you happen to find a security vulnerability, we would appreciate you letting us know at https://hackerone.com/automattic and allowing us to respond before disclosing the issue publicly.

## Getting in Touch

If you have questions or just want to say hi, join the [WordPress Slack](https://chat.wordpress.org) and drop a message on the `#mobile` channel.

## Documentation

- [Coding Style](docs/coding-style.md) - guidelines and validation and auto-formatting tools
- [Pull Request Guidelines](docs/pull-request-guidelines.md) - branch naming and how to write good pull requests

Please read the [docs](docs/) for more.

## Resources

- [WordPress Mobile Blog](http://make.wordpress.org/mobile)
- [WordPress Mobile Handbook](http://make.wordpress.org/mobile/handbook/)

## License ##

WordPress for Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.

[1]: https://play.google.com/store/apps/details?id=org.wordpress.android
[3]: https://developer.android.com/studio
[4]: https://make.wordpress.org/chat/
[5]: https://developer.wordpress.com/apps/
[6]: https://developer.wordpress.com/docs/oauth2/
[7]: https://developer.wordpress.com/docs/api/
[8]: https://developers.google.com/identity/
