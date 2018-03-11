# WordPress-Utils-Android

Collection of utility methods for Android and WordPress.

## Use the library in your project

* In your build.gradle:
```groovy
dependencies {
    compile 'org.wordpress:utils:1.19.0' // use version 1.19.0
}
```

## Publish an updated version to your local maven repository

You can bump the [version name in the main build file: `WordPressUtils/build.gradle`][1]. After updating the build file, you can build, and publish the library to your local maven repo. That will let you try the new version in your app for example.

```shell
$ ./gradlew assemble test publishToMavenLocal
```

## Publish it to Bintray

When a new version is ready to be published to the remote repository, use the following command to upload it to Bintray:

```shell
$ ./gradlew assemble test bintrayUpload -PbintrayUser=FIXME -PbintrayKey=FIXME -PdryRun=false
```

## Apps and libraries using WordPress-Utils-Android:

- [WordPress for Android][2]
- [FluxC][3]

## License
Dual licensed under MIT, and GPL.

[1]: https://github.com/wordpress-mobile/WordPress-Utils-Android/blob/a9fbe8e6597d44055ec2180dbf45aecbfc332a20/WordPressUtils/build.gradle#L37
[2]: https://github.com/wordpress-mobile/WordPress-Android
[3]: https://github.com/wordpress-mobile/WordPress-FluxC-Android
