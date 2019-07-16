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
    
    
## Creating a mock file 

The JSON files used by WireMock to handle requests and are located in `src/main/assets`.  To generate one of these files 
you're first going to want to set up [Charles Proxy](https://www.charlesproxy.com/) (or similar) to work with your Android emulator.
 If you've never done this I found 
[this article](https://medium.com/@daptronic/the-android-emulator-and-charles-proxy-a-love-story-595c23484e02) to be a 
good place to start. Once you've done that you'll want to walk through the specfic flow you're testing and store the JSON contents
of the necessary responses in the `jsonBody` field of the `response` field in the mock file. 

Here's an example of what this might look like,

```
{
    "request": {
        "urlPattern": "/rest/v1.1/me/",
        "method": "GET"
    },
    "response": {
        "status": 200,
        "jsonBody": {
            // ..... Your response here
        },
        "headers": {
            "Content-Type": "application/json",
            "Connection": "keep-alive",
            "Cache-Control": "no-cache, must-revalidate, max-age=0"
        }
    }
}
``` 

These files are used to match network requests while the tests are being run. For more on request matching with 
WireMock check out [their documentation](http://wiremock.org/docs/request-matching/). 

## License ##

WordPressMocks is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md).
