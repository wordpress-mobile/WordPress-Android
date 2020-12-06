# Frequently Asked Questions

#### I can't build/test/package the project because of a `PermGen space` error.

Create a `gradle.properties` file in the project root directory with the
following: `org.gradle.jvmargs=-XX:MaxPermSize=1024m`.

#### Unable to build on Windows

When building on Windows, you may encounter the following error in Android Studio:

```
The syntax of the command is incorrect.
npm ERR! code ELIFECYCLE
npm ERR! errno 1
...
Execution failed for task ':@wordpress_react-native-bridge:buildJSBundle'.
```

The workaround is to disable bundling the JS by
[setting `wp.BUILD_GUTENBERG_FROM_SOURCE=true`](https://github.com/wordpress-mobile/WordPress-Android/issues/12120#issuecomment-643431502)
in the top-level `gradle.properties` file.

There is additional context in issue
[#12120](https://github.com/wordpress-mobile/WordPress-Android/issues/12120).
