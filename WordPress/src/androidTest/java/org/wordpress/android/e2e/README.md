## UI tests Overview

WordPress for Android has UI acceptance tests for critical user flows through the app, such as login,
signup, and publishing. The tests use mocked network requests with [WireMock](http://wiremock.org/).

## Running the tests

**Note that due to the mock server setup, tests cannot be run on physical devices right now.**


1. Follow the [build instructions](https://github.com/wordpress-mobile/WordPress-Android#build-instructions)
 (steps 1-7) to clone, build and run the project in Android Studio.
2. Run the tests in `org.wordpress.android.e2e`

There's no additional steps needed to make the tests run against the mock server -- that's configured by default.

## Adding a new test?

Great! When adding a new UI test, consider:

* Whether you need to test a user flow (to accomplish a task or goal) or a specific feature (e.g. boundary testing).
* What screens/pages are being tested (defined in `pages/`).
* What user flows are being used (defined in `flows/`).
* Any specific UI components being interacted with?(defined in `components/`).
* What network requests are made during the test (defined in `libs/mocks`)).

It's preferred to focus UI tests on entire user flows, and group tests with related flows or goals in the same test file.

When you add a new test, you may need to add new screens, methods, and flows. When writing these I encourage you to check
`support/WPSupportUtils.java` for additional code to reuse to automate the task. Our tests are also used to generate screenshots
and a lot of useful helper functions are made available through that auxillary file. Wherever possible, avoid the use
 of a string to select a UI element on the screen; the use of other identifiers such as id or content description is preferable
  and should be used where possible, even if that means adding it to a UI element that might be missing the appropriate field.
   This ensures tests can be run regardless of the device language.

## Adding or updating network mocks

When you add a test (or when the app changes), the request definitions for WireMock need to be updated. You can read WireMock’s documentation [here](http://wiremock.org/docs/).

If you are unsure what network requests need to be mocked for a test, an easy way to find out is to run the app through [Charles Proxy](https://www.charlesproxy.com/) and observe the required requests.

`mocks` is included as a module in `WordPress-Android` and are located in `libs/mocks/`; you can update
your local mock files and make changes here.
