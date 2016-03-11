# WordPress-Stores-Android

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-Stores-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-Stores-Android)

WordPress-Stores-Android is a networking and persistence library that helps to connect and sync data from a WordPress site (self hosted, or wordpress.com site). It's not ready for prime time yet.

Based on the [Flux](https://facebook.github.io/flux/docs/overview.html) pattern, we're using: [Dagger2](https://google.github.io/dagger/) for dependency injection, [WellSql](https://github.com/yarolegovich/wellsql) for persistence.

## Building the library

The [gradle build system][2] will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR with your actual android SDK directory)
file and create the gradle.properties file. The easiest way is to copy
our example:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ ./gradlew WordPressStores:build

## Building and running tests and the example app

    $ cp example/gradle.properties-example example/gradle.properties
    $ cp example/tests.properties-example example/tests.properties
    $ ./gradlew cAT // Regression tests
    $ ./gradlew testDebug // Unit tests

Note: this is the default `example/gradle.properties` file. You'll have to get
a WordPress.com OAuth2 ID and secret.

We have some tests connecting to real HTTP servers, URL and credentials are defined in `example/tests.properties`, you must edit it or obtain the real file to run the tests. This is temporary.

## Need help to build or hack?

Say hello on our [Slack][4] channel: `#mobile`.

## LICENSE

WordPress-Stores-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).

[4]: https://make.wordpress.org/chat/
