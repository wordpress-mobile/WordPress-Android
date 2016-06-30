# WordPress-Editor-Android #

[![Build Status](https://travis-ci.org/wordpress-mobile/WordPress-Editor-Android.svg?branch=develop)](https://travis-ci.org/wordpress-mobile/WordPress-Editor-Android)

## Introduction ##

WordPress-Editor-Android is the text editor used in the [WordPress Android app](https://github.com/wordpress-mobile/WordPress-Android) to create and edit pages & posts. In short it's a simple, straightforward way to visually edit HTML.

## Build Instructions ##

Post-checkout instructions for Windows, necessary to convert the assets symlink to a Windows symlink:

From git bash, inside the cloned project root:

    $ rm WordPressEditor/src/main/assets
    $ git ls-files --deleted -z | git update-index --assume-unchanged -z --stdin

Then, from a Windows command prompt:

    mklink /D [PROJECT_ROOT]\WordPressEditor\src\main\assets %PROJECT_ROOT%\libs\editor-common\assets

Finally, update `[PROJECT_ROOT]\.git\info\exclude` to ignore the symlink locally:

    # assets symlink
    WordPressEditor/src/main/assets

## Testing ##

This project has both unit testing and integration testing, maintained and run separately.

Unit testing is done with the [Robolectric framework](http://robolectric.org/). To run unit tests simply run `gradlew testDebug`.

Integration testing is done with the [Android testing framework](http://developer.android.com/tools/testing/testing_android.html). To run integration tests run `gradlew connectedAndroidTest`.

Add new unit tests to `src/test/java/` and integration tests to `stc/androidTest/java/`.

### JavaScript Tests ###

This project also has unit tests for the JS part of the editor using [Mocha](https://mochajs.org/).

To be able to run the tests, [npm](https://www.npmjs.com/) and Mocha (`npm install -g mocha`) are required.

With npm and Mocha installed, from within `libs/editor-common/assets/test`, run:

    npm install chai

And then run `mocha` inside `libs/editor-common/assets`:

    cd libs/editor-common/assets; mocha; cd -

## LICENSE ##

WordPress-Editor-Android is an Open Source project covered by the [GNU General Public License version 2](LICENSE.md).
