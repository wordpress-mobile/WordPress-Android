# WordPress-Stores-Android

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
    $ ./gradlew cAT

Note: this is the default `example/gradle.properties` file. You'll have to get
a WordPress.com OAuth2 ID and secret.

## Need help to build or hack?

Say hello on our [Slack][4] channel: `#mobile`.

## LICENSE

WordPress-Stores-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).

[4]: https://make.wordpress.org/chat/
