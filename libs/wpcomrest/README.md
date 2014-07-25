# WordPress REST Client for Android

## Build

To build the library, invoke the following `gradle` command in the project root directory:

    $ ./gradlew build

This will create an `aar` package at this location: `WordPressComRest/build/outputs/aar/WordPressComRest.aar`. Feel free to use it directly or put it in a maven repository.

## Usage

If you don't want to compile and host it. The easiest way to use it in your Android project is to add it as a library in your build.gradle file, don't forget to add the wordpress-mobile maven repository. For instance:

    repositories {
        maven { url 'http://wordpress-mobile.github.io/WordPress-Android' }
    }

    dependencies {
        // use the latest 1.x version
        compile 'com.automattic:wordpresscom-rest:1.+'
    }

## LICENSE

This library is dual licensed unded MIT and GPL v2.
