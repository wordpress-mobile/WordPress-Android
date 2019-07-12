## UI tests Overview

WordPress for Android has UI acceptance tests for critical user flows through the app, such as login, 
signup, and publishing. The tests use mocked network requests with [WireMock](http://wiremock.org/), 
defined in [WordPressMocks](https://github.com/wordpress-mobile/WordPressMocks).

## Running the tests 

**Note that due to the mock server setup, tests cannot be run on physical devices right now.**


1. Follow the [build instructions](https://github.com/wordpress-mobile/WordPress-Android#build-instructions)
 (steps 1-7) to clone, build and run the project in Android Studio.
2. Run the tests  `/Users/javon/Automattic-Projects/WordPress-Android/WordPress/src/androidTest/java/org/wordpress/android/e2e`