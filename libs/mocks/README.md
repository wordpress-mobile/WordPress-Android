# Mocks

Module for network Mocking purposes during instrumentation testing.

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
