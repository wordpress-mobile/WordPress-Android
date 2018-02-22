# WordPress-Lint-Android
Pluggable lint module for WordPress-Android.

## Use the library in your project

* In your build.gradle:
```groovy
dependencies {
    compile 'org.wordpress:lint:1.0.0' // use version 1.0.0
}
```

## Publish an updated version to your local maven repository

You can bump the [version name in the main build file: `WordPressLint/build.gradle`][1]. After updating the build file, you can build, and publish the library to your local maven repo. That will let you try the new version in your app for example.

```shell
$ ./gradlew assemble test publishToMavenLocal
```

## Publish it to Bintray

When a new version is ready to be published to the remote repository, use the following command to upload it to Bintray:

```shell
$ ./gradlew assemble test bintrayUpload -PbintrayUser=FIXME -PbintrayKey=FIXME -PdryRun=false
```

## License ##

WordPress-Login-Flow-Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md).
