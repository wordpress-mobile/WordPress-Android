# 2FA Login Issue #

Due to a known [issue](https://github.com/wordpress-mobile/WordPress-Android/issues/8754), two-factor authentication is not currently fully supported on development builds.

To workaround this issue, please either login with a wordpress account without 2FA authentication enabled or, if you must log in with an account with 2FA enabled, you can use the following workaround.

1. Login with the 2FA enabled account as normal following the steps detailed [here](https://github.com/AmirGamilDev/WordPress-Android#oauth2-authentication).
2. After entering your 6-digit verification code and tapping "NEXT", you will receive an error message stating "couldn't retrieve your profile".  You are now logged into the app.  If you perform a full close of the app (for example, using the back button to clear the back stack) and open it again you should find yourself logged in but you won't see sites associated with this account.  You can manually add self-hosted sites at this point.  Or, if you wish to auto-retrieve the sites, continue on to step 3.
3. Create another client id and secret pair using a different wordpress.com account that doesn't have 2FA enabled as described in the [OAuth2 Authentication](https://github.com/AmirGamilDev/WordPress-Android#oauth2-authentication) section of the Readme.
4. Change the values in your gradle.properties file to these new values.
5. Sync your project, build and deploy the app.  DO NOT CLEAR DATA on the app.
6. Open the app as before.  You will be logged in and your full profile information will be retrieved along with any sites.
