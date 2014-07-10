# WordPress REST Client for Android

## Usage

Easiest way to install the library is by building the latest `jar` and dropping it in your project's `./lib` directory.

    cd path/to/library
    ant jar

This will produce two files:
- `dist/wordpress-android-rest-VERSION.jar`
- `dist/wordpress-android-rest-VERSION-http.jar`


You can also use this project as an Android Library Project instead of using the `jar` by adding a library references to your app's `project.properties` file:

    android.library.reference.1=path/to/wordpress-rest-android/

## LICENSE

This library is dual licensed unded MIT and GPL v2.
