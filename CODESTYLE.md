# Code Style Guidelines for WordPress-Android

Our code style guidelines is based on the [Android Code Style Guidelines for Contributors](https://source.android.com/source/code-style.html). We only changed a few rules:

* Line length is 120 characters
* FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

You can run a checkstyle with most rules via a gradle command:

    $ ./gradlew checkstyle

It generates a HTML report in `build/reports/checkstyle/checkstyle-result.html`.