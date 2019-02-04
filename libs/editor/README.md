# WordPress-Editor-Android #

## Introduction ##

WordPress-Editor-Android is the text editor used in the [WordPress Android app](https://github.com/wordpress-mobile/WordPress-Android) to create and edit pages & posts. In short it's a simple, straightforward way to visually edit HTML.

## Testing ##

This project has both unit testing and integration testing, maintained and run separately.

Unit testing is done with the [Robolectric framework](http://robolectric.org/). To run unit tests simply run `gradlew testDebug`.

Integration testing is done with the [Android testing framework](http://developer.android.com/tools/testing/testing_android.html). To run integration tests run `gradlew connectedAndroidTest`.

Add new unit tests to `src/test/java/` and integration tests to `stc/androidTest/java/`.

### JavaScript Tests ###

This project also has unit tests for the JS part of the editor using [Mocha](https://mochajs.org/).

To be able to run the tests, [npm](https://www.npmjs.com/) and Mocha (`npm install -g mocha`) are required.

With npm and Mocha installed, from within `example/src/test/js`, run:

    npm install chai

And then run `mocha` inside the same folder:

    cd example/src/test/js; mocha test*; cd -

## LICENSE ##

WordPress-Editor-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).
