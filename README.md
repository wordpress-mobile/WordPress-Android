# WordPress for Android #

[![CircleCI](https://circleci.com/gh/wordpress-mobile/WordPress-Android.svg?style=svg)](https://circleci.com/gh/wordpress-mobile/WordPress-Android)

If you're just looking to install WordPress for Android, you can find
it on [Google Play](https://play.google.com/store/apps/details?id=org.wordpress.android&referrer=utm_source%3Dgithub%26utm_medium%3Dwebsite). If you're a developer wanting to contribute, read on.


## Build Instructions ##

1. Make sure you've installed [Android Studio](https://developer.android.com/studio/index.html).
1. `git clone --recurse-submodules git@github.com:wordpress-mobile/WordPress-Android.git` in the folder of your preference.
Or if you already have the project cloned, initialize and update the submodules:
```
git submodule init
git submodule update
```
1. `cd WordPress-Android` to enter the working directory.
1. `cp gradle.properties-example gradle.properties` to set up the sample app credentials file.
1. `git submodule update --init --recursive`  to pull the submodules (optionally use `--depth=1 --recommend-shallow` flags to skip pulling full submodules' history).
1. In Android Studio, open the project from the local repository. This will auto-generate `local.properties` with the SDK location.
1. Go to Tools → AVD Manager and create an emulated device.
1. Run.

Notes:

* To use WordPress.com features (login to WordPress.com, access Reader and Stats, etc) you need a WordPress.com OAuth2 ID and secret. Please read the [OAuth2 Authentication](#oauth2-authentication) section.
* While loading/building the app in Android Studio ignore the prompt to update the gradle plugin version as that will probably introduce build errors. On the other hand, feel free to update if you are planning to work on ensuring the compatibility of the newer version.


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
`wp.oauth.app_id` and `wp.oauth.app_secret` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account. Note that authenticating to WordPress.com via Google is not supported 
in development builds of the app, only in the official release.

Note that credentials created with our [WordPress.com applications manager][5] 
allow login only and not signup. New accounts must be created using the [official app][1] 
or [on the web](https://wordpress.com/start). Login is restricted to the WordPress.com 
account with which the credentials were created. In other words, if the credentials 
were created with foo@email.com, you will only be able to login with foo@email.com. 
Using another account like bar@email.com will cause the `Client cannot use "password" grant_type` error. 

For security reasons, some account-related actions aren't supported for development 
builds when using a WordPress.com account with 2-factor authentication enabled.

Read more about [OAuth2][6] and the [WordPress.com REST endpoint][7].

## Build and Test ## 

To build, install, and test the project from the command line:

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
- [Subtree'd Library Projects](docs/subtreed-library-projects.md) - how to deal with subtree dependencies

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
[3]: http://developer.android.com/sdk/installing/studio.html
[4]: https://make.wordpress.org/chat/
[5]: https://developer.wordpress.com/apps/
[6]: https://developer.wordpress.com/docs/oauth2/
[7]: https://developer.wordpress.com/docs/api/
[8]: https://developers.google.com/identity/
