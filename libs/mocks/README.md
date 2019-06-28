# WordPressMocks

Network mocking for testing the WordPress mobile apps

## Usage ##

### Android

To use this library in your project, you must set it up as a subtree.
From the root of your main project, add the subtree:

    $ git subtree add --prefix=libs/mocks git@github.com:wordpress-mobile/WordPressMocks.git develop

This will create a new directory, `libs/mocks`, containing the contents of this repository.

At this point, you can add it to your project's root `settings.gradle`:

```groovy
include ':libs:mocks:WordPressMocks'
```

and to your main app module's `build.gradle` dependencies:

```groovy
dependencies {
    androidTestImplementation('com.github.tomakehurst:wiremock:2.23.2') {
        exclude group: 'com.fasterxml.jackson.core', module: 'jackson-core'
        exclude group: 'org.apache.httpcomponents', module: 'httpclient'
        exclude group: 'org.apache.commons', module: 'commons-lang3'
        exclude group: 'asm', module: 'asm'
        exclude group: 'org.json', module: 'json'
    }
    androidTestImplementation 'org.apache.httpcomponents:httpclient-android:4.3.5.1'
    androidTestImplementation project(path:':libs:mocks:WordPressMocks')
}
```

### Standalone

To start the WireMock server as a standalone process, you can run it with this command:

```
./scripts/start.sh 8282
```

Here `8282` is the port to run the server on. It can now be accessed from `http://localhost:8282`.

## Contributing ##

You can fetch the latest changes made to this library into your project using:

    $ git subtree pull --prefix=libs/mocks git@github.com:wordpress-mobile/WordPressMocks.git develop --squash

And you can push your own changes upstream to `WordPressMocks` using:

    $ git subtree push --prefix=libs/mocks git@github.com:wordpress-mobile/WordPressMocks.git branch-name

Note: You can add this repository as a remote to simplify the `git subtree push`/`pull` commands:

    $ git remote add mockslib git@github.com:wordpress-mobile/WordPressMocks.git

This will allow to use this form instead:

    $ git subtree pull --prefix=libs/mocks mockslib develop --squash

## License ##

WordPressMocks is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md).
