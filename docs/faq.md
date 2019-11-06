# Frequently Asked Questions

#### I can't build/test/package the project because of a `PermGen space` error.

Create a `gradle.properties` file in the project root directory with the
following: `org.gradle.jvmargs=-XX:MaxPermSize=1024m`.