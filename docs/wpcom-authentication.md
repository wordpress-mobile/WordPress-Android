# WordPress.com Authentication

WordPress.com features (signing in with a WordPress.com account, the Reader, Stats,
etc.) are authenticated with OAuth2, which requires a client ID and a client secret.
These details are used to authenticate your application and verify that the API calls
being made are valid. The credentials in the repository are placeholders, so you need to
register your own application and point the app at it.

1. Create an application with the [WordPress.com applications manager][1]. Select
   "**Native client**" for the application type.

2. Add both of the following to "**Redirect URLs**", one per line:

       wordpress://wpcom-authorize
       jetpack://wpcom-authorize

   Authentication takes place in the browser and returns to the app through a URL
   scheme, which differs per app: `wordpress` for WordPress and `jetpack` for Jetpack.
   WordPress.com only redirects to a URL that exactly matches a registered one, so
   register both if you intend to run both apps. The "**Website URL**" and
   "**Javascript Origins**" fields are required by the form but aren't used by the
   apps. Just use "**[https://localhost](https://localhost)**".

3. From the root of the repository, copy the example configuration file.
   `secrets.properties` is ignored by Git, so your credentials stay out of version
   control:

       $ cp secrets.properties-example secrets.properties

4. In `secrets.properties`, uncomment `wp.oauth.app_id` and `wp.oauth.app_secret` and
   set them to the "Client ID" and "Client Secret" of the application you created.

5. Rebuild the app, then choose "Log in or sign up with WordPress.com" on the app start
   up screen.

Note that authenticating to WordPress.com via Google is not supported in development
builds of the app, only in the official release. See the
[Google Configuration](../README.md#google-configuration) section of the README for
details.

Read more about [OAuth2][2] and the [WordPress.com REST endpoint][3].

[1]: https://developer.wordpress.com/apps/
[2]: https://developer.wordpress.com/docs/oauth2/
[3]: https://developer.wordpress.com/docs/api/
