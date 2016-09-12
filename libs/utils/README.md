# WordPress-Utils-Android

Collection of utility methods for Android and WordPress.

## Use the library

* In your build.gradle:
```groovy
dependencies {
    // use the latest 1.x version
    compile 'org.wordpress:utils:1.+'
}
```

## Publish it to bintray

```shell
$ ./gradlew assemble publishToMavenLocal bintrayUpload -PbintrayUser=FIXME -PbintrayKey=FIXME -PdryRun=false
```

## Apps that use this library
- [WordPress for Android][1]

## License
Dual licensed under MIT, and GPL.

[1]: https://github.com/wordpress-mobile/WordPress-Android
