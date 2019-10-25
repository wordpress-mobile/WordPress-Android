Our code style guidelines are based on the [Android Code Style Guidelines for Contributors](https://source.android.com/source/code-style.html). We only changed a few rules:

* Line length is 120 characters
* FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

On top of the Android linter rules (best run for this project using `./gradlew lintVanillaRelease`), we use two linters: [Checkstyle](http://checkstyle.sourceforge.net/) (for Java and some language-independent custom project rules), and [ktlint](https://github.com/pinterest/ktlint) (for Kotlin).

## Checkstyle

You can run checkstyle via a gradle command:

```
$ ./gradlew checkstyle
```

It generates an HTML report in `WordPress/build/reports/checkstyle/checkstyle.html`.

You can also view errors and warnings in realtime with the Checkstyle plugin.  When importing the project into Android Studio, Checkstyle should be set up automatically.  If it is not, follow the steps below.

You can install the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the custom configuration file, located at [config/checkstyle.xml](https://github.com/wordpress-mobile/WordPress-Android/blob/develop/config/checkstyle.xml).

## ktlint

You can run ktlint using `./gradlew ktlint`, and you can also run `./gradlew ktlintFormat` for auto-formatting. There is no IDEA plugin (like Checkstyle's) at this time.
