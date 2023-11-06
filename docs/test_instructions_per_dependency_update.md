# Test Instructions per Dependency Update

## Purpose

The purpose of this document is to establish a set of guidelines for testing the app during
dependency updates. Specifically, it outlines a series of test instructions that should be adhered
to when updating a dependency.

## FAQ

### Could someone overlook testing a dependency update on a new screen?

This document focuses on a set of screens that are essential to the app's functionality. The concept
is that, to ensure an update's safety, there's no need to test every screen using that dependency;
testing the core ones should suffice.

### How much effort will it take to maintain this document?

Engineers should only update this document if new core screens are added, potentially introducing
regressions if left untested, or if an existing core screen is removed, rendering the dependency
update redundant. If an engineer observes that a core screen has been updated, they should revise
the testing instructions as part of the dependency update pull request. This makes keeping this
document up-to-date a low-effort and minimally maintenance-intensive task.

### Should this document be blindly trusted?

No engineer should blindly trust and strictly adhere to the testing instructions provided in this
document. These instructions serve as a starting point and a reminder for engineers working on a
dependency update to ensure they test the core screens. Engineers should exercise their best
judgment and conduct additional testing if they deem it necessary.

### Do we need testing instructions for every dependency update?

Some dependency updates are quite broad and apply to the entire app, making it challenging to
provide specific testing instructions (e.g., `fragment`, `appcompat`, `compose`, etc.). In such
cases, engineers should use their best judgment to perform a smoke test on the entire app to ensure
its correctness. Engineers should rely on their intuition to identify which dependencies warrant
testing instructions. Therefore, this document should be regarded as a set of helpful guidelines
rather than strict requirements.

## Content

1. [WorkManager](#workmanager)
2. [Preference](#preference)
3. [ExoPlayer](#exoplayer)
4. [Firebase](#firebase)
5. [PlayServicesAuth](#playservicesauth)
6. [PlayServicesCoreScanner](#playservicescodescanner)
7. [Navigation](#navigation)
8. [Okio](#okio)
9. [Zendesk](#zendesk)
10. [JSoup](#jsoup)

ℹ️ Every test instruction should be prefixed with one of the following:
- [JP/WP] This test applies to both, the `Jetpack` and `WordPress` apps.
- [JP] This test applies to the `Jetpack` app only.
- [WP] This test applies to the `WordPress` app only.

### WorkManager [[androidxWorkManagerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L54)] <a name="workmanager"></a>

<details>
  <summary>1. [JP/WP] UploadWorker.kt</summary>

- Go to `Post` screen.
- Create a new post and publish it.
- Turn device offline.
- Go to this post and update it.
- Notice the warning message: `We'll publish the post when your device is back online.`
- Turn device online.
- Notice this post being automatically uploaded.
- Open this post on a web browser and verify the post is indeed updated.

</details>

<details>
  <summary>2. [JP] ReminderWorker.kt</summary>

- Go to `Site Settings` screen.
- Find the `Blogging` section, click on `Reminders`, toggle-on every day and click on `Update`.
- Notice the `All set!` bottom sheet appearing, click `Done`.
- Close the app, preferably swipe the app off.
- Go to the device's `Settings` app, find the `Date & Time` section, turn `Automatic date & time`
  off.
- Set the device's date to a day after today.
- Open the app.
- Verify the `Blogging Reminders` notification appearing. For example, the notification title could
  be `Daily Prompt`, while the notification description something like `Is there anything you feel
  too old to do anymore?`.

</details>

<details>
  <summary>3. [JP] LocalNotificationWorker.kt</summary>

- TODO
- TODO
- TODO

</details>

<details>
  <summary>4. [JP] WeeklyRoundupWorker.kt</summary>

- TODO
- TODO
- TODO

</details>

### Preference [[androidxPreferenceVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L50)] <a name="preference"></a>

<details>
  <summary>1. [JP/WP] Account Settings [AccountSettingsFragment.kt]</summary>

- Go to `Me` tab.
- Click on the `Account Settings` button.
- Verify that the `Account Settings` screen is displayed.
- Click on each of the settings within the `Account Settings` screen and verify that every setting
  works as expected.

</details>

<details>
  <summary>2. [JP/WP] App Settings [AppSettingsFragment.java]</summary>

- Go to `Me` tab.
- Click on the `App Settings` button.
- Verify that the `App Settings` screen is displayed.
- Click on each of the settings within the `App Settings` screen and verify that every setting works
  as expected, including the inner settings like the `Privacy Settings`.
- Do the same for the `Debug Settings` screens.

</details>

<details>
  <summary>3. [JP/WP] Site Settings [SiteSettingsFragment.java]</summary>

- Go to `Site Settings` screen.
- Verify that the `Site Settings` screen is displayed.
- Click on each of the settings within the `Site Settings` screen and verify that every setting
  works as expected.

</details>

<details>
  <summary>4. [JP/WP] Jetpack Settings - Security [JetpackSecuritySettingsFragment.java]</summary>

ℹ️ Prerequisite: To have this setting displayed you must have a Jetpack connected site.

- Go to `Site Settings` screen.
- Find the `Jetpack Settings` section and click on `Security`.
- Verify that the `Security` setting screen is displayed.
- Click on each of the settings within the `Security` settings screen and verify that every setting
  works as expected.

</details>

<details>
  <summary>5. [JP] Notifications Settings [NotificationsSettingsFragment.java]</summary>

- Go to `Notifications` tab.
- Click on the `Gear` setting button (top-right).
- Verify that the `Notification Settings` screen is displayed.
- Click on each of the settings within the `Notification Settings` settings screen and verify that
  every setting works as expected.

</details>

<details>
  <summary>6. [JP/WP] Edit Post Screen [EditPostActivity.java]</summary>

ℹ️ Editing a new post uses `PreferenceManager` to `setDefaultValues(...)` for `Account Settings`.

- Go to `Post` screen.
- Edit a new post, add a few of the main blocks and verify that everything is workings as expected.

</details>

### ExoPlayer [[googleExoPlayerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="exoplayer"></a>

<details>
  <summary>1. [JP/WP] Image Preview</summary>

- Go to `Post` screen.
- Edit a new post, add an `Image` block and then an image into it.
- Having added an image to the image block, click on the image block and then again on the image within.
- This will launch `Media Preview` screen and the image should be previewed.
- Verify that the `Image Preview` is working as expected, that rotating the device works and finally
  that clicking back navigates you back to the post edit screen.

</details>

<details>
  <summary>2. [JP/WP] Video Preview</summary>

- Go to `Post` screen.
- Edit a new post, add an `Video` block but don't add a video into it just yet.
- Find the `Video` block, click on `ADD VIDEO` and then `Choose from device` to choose a video.
- From the list of available videos to choose from screen, click on the `Play` button on the center
  on any video. FYI: If you don't click on the center, this video will get selected for use, but it
  won't play.
- This will launch `Media Preview` screen and the video should start playing.
- Verify that the `Video Preview` is working as expected, that rotating the device works and finally
  that clicking back navigates you back to the list of available videos to choose from screen.

</details>

### Firebase [[firebaseBomVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="firebase"></a>

<details>
    <summary>1. [JP/WP] Push Notification on Post Being Published [GCMMessageService.java]</summary>

- Add a new `blog` post.
- Add any number of blocks.
- Publish the post.
- Verify that, on post being published, you get a push notification with the post's title, which,
  when you click on it, navigates you to the 'Notifications Detail' screen for this post.

</details>

### PlayServicesAuth [[googlePlayServicesAuthVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="playservicesauth"></a>

<details>
  <summary>1. [JP] Google Sign-in on Jetpack</summary>

ℹ️ Prerequisite: If you don't have access to upload an`.aab` to Google Play Store for
   `Jetpack - Website Builder`, post a request to `+systemsrequests`.

- Check-out this branch and edit the `version.properties` file to pick a dummy `versionName` and
  `versionCode`, preferable using a higher `versionCode` to the existing one.
  - I recommend using a `versionCode` of `1000000001` and above.
  - I recommend using an explicit `versionName` like `deps-update-google-play-services-20-4-1`
    (similar to the name of this branch itself)
- Build the release `.aab` locally using this command: `./gradlew bundleJetpackVanillaRelease`
- Upload the locally generated `.aab`, found within the `WordPress/build/outputs/bundle/jetpackVanillaRelease/`
  build folder, and into Google Play Console (`Jetpack - Website Builder`) via:
  `App Bundle Explorer` > `Upload new version` (top-right)
  - Wait for the upload to complete, then via this newly uploaded app version, navigation to the
    `Downloads`, on the `Assets` section, find the `Signed, universal APK` and click the `Download`
    icon (middle-right) to download the signed `.apk` locally.
  - After having this signed `.apk` downloaded locally, go ahead and click on the `Delete app bundle`
    to delete the this app bundle from the list of available app bundles.
- Install this download signed `.apk` using this command:
  `adb install -r Downloads/jpandroid-deps-update-google-play-services-20-4-1-Signed.apk`
  (note that you can use `./tools/rename_apk_aab.sh` to rename the download signed `.apk`)
- If you were already logged-in to Jetpack, log-out and log-in again using the `Continue with Google`
  button (note that you need to use a non `A8C` account to be able to complete this flow).
- Verify that the Google sign-in works, that you have successfully logged-in and are able to use the
  app as expected.

</details>

<details>
  <summary>2. [WP] Google Sign-in on WordPress</summary>

ℹ️ Prerequisite: If you don't have access to upload an`.aab` to Google Play Store for
   `WordPress - Website Builder`, post a request to `+systemsrequests`.

- Check-out this branch and edit the `version.properties` file to pick a dummy `versionName` and
  `versionCode`, preferable using a higher `versionCode` to the existing one.
  - I recommend using a `versionCode` of `1000000001` and above.
  - I recommend using an explicit `versionName` like `deps-update-google-play-services-20-4-1`
    (similar to the name of this branch itself)
- Build the release `.aab` locally using this command: `./gradlew bundleWordPressVanillaRelease`
- Upload the locally generated `.aab`, found within the `WordPress/build/outputs/bundle/wordpressVanillaRelease/`
  build folder, and into Google Play Console (`WordPress - Website Builder`) via:
  `App Bundle Explorer` > `Upload new version` (top-right)
  - Wait for the upload to complete, then via this newly uploaded app version, navigation to the
    `Downloads`, on the `Assets` section, find the `Signed, universal APK` and click the `Download`
    icon (middle-right) to download the signed `.apk` locally.
  - After having this signed `.apk` downloaded locally, go ahead and click on the `Delete app bundle`
    to delete the this app bundle from the list of available app bundles.
- Install this download signed `.apk` using this command:
  `adb install -r Downloads/wpandroid-deps-update-google-play-services-20-4-1-Signed.apk`
  (note that you can use `./tools/rename_apk_aab.sh` to rename the download signed `.apk`)
- If you were already logged-in to WordPress, log-out and log-in again using the `Continue with Google`
  button (note that you need to use a non `A8C` account to be able to complete this flow).
- Verify that the Google sign-in works, that you have successfully logged-in and are able to use the
  app as expected.

</details>

### PlayServicesCodeScanner [[googlePlayServicesCodeScannerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="playservicescodescanner"></a>

<details>
  <summary>1. [JP] Scan Login Code</summary>

Step.1:
- Build and install the `Jetpack` app (note that you don't need a release build, a debug build will
  suffice).
- Login to the `Jetpack` app with a `WP.com` account (note that you need to use a non `A8C` account
  and a non `2FA` enabled account).
- Navigate to the `Me` screen (click on avatar at top-right).
- (STOP)

Step.2:
- Head over to your desktop and open a web browser (note that using an incognito tab works best).
- Browse to `wordpress.com` (note that if you are logged-in, log-out first).
- Tap the `Log In` link (top-right).
- Tap the `Login via the mobile app` link in the list of options below the main `Continue` button
  (bottom-middle).
- Verify you are on the `Login via the mobile app` view and `Use QR Code to login` is shown, along
  with a QR code for you to scan.
- (STOP)

Step.3:
- Head back to your mobile.
- Tap the `Scan Login Code` item on the `Me` screen you are currently at.
- Scan the QR code on the web browser.
- Follow the remaining prompts on your mobile to login to WordPress on your web browser (desktop),
  verify that you have successfully logged-in and are able to use WordPress as expected.

</details>

### Navigation [[navigationVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="navigation"></a>

<details>
    <summary>1. [JP/WP] Image Editing Flow [libs:image-editor]</summary>

- Add a new `blog` post.
- Add a new `image` block.
- Choose an image and wait for it to be uploaded within the `image` block.
- Click on the `media options` of this image (top right) and then click `edit`.
- Verify that the `Edit Image` screen is shown and functioning as expected.
- Crop the image and click the `done` menu option (top right).
- Make sure the image is updated accordingly.

</details>

### Okio [[squareupOkioVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="okio"></a>

<details>
    <summary>1. [JP/WP] Me Screen [GravatarApi.java + StreamingRequest.java]</summary>

- Go to `Me` tab.
- From the `Me` screen you are in, click on your profile's icon (`CHANGE PHOTO`).
- Choose an image and wait for the `Edit Photo` screen to appear.
- Crop the image and click the `done` menu option (top right).
- Verify the image is updated accordingly.

</details>

### Zendesk [[zendeskVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="zendesk"></a>

<details>
    <summary>1. [JP] Zendesk Update [Tickets & Contact Support]</summary>

ℹ️ This test only works when testing with a normal, non-a8c user account.

- Go to `Me` tab -> `Help` -> `Tickets`.
- Verify that the `My tickets` Zendesk related screen opens and that it lists all of your tickets
  (or non if you don't have any yet).
- Go back to the `Help` settings screen and then click on `Contact Support`.
- Verify that the `Help` Zendesk related screen opens and that you are prompt to `Send a message...`.
- Type `Testing Zendesk X.Y.Z update, please ignore and thank you!` and then send your message.
- Go back to the `Help` settings screen and then click on `Tickets` again.
- Verify that your previously sent message is listed in there and that you can click on it.
  PS: You could also check your emails and verify that you got a
  `Thank you for contacting the WordPress.com support team! (#1234567)` email.
- Verify that clicking on it navigates you to the inner screen where your message is shown as
  `Delivered` along with a predefined automated `mobile support` response message.

</details>

### JSoup [[jsoupVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="jsoup"></a>

<details>
    <summary>1. [JP/WP] Classic Editor [Aztec]</summary>

ℹ️ Disable the `Block Editor` before testing this.

- Go to `Posts` screen and create a new post.
- Add an `Image`, `Video` and any other entry (like `Paragraph`, `Ordered List`, `Quote`, etc).
- Publish this newly created post.
- Verify that this newly created `Classic Editor` related post of yours is being displayed as
  expected, both when previewing it from within the `Posts` and `Reader` screens. FYI: On `Reader`,
  you will find you post within the `FOLLOWING` tab.

</details>

<details>
    <summary>2. [JP/WP] Block Editor [Gutenberg]</summary>

ℹ️ Enable the `Block Editor` before testing this.

- Go to `Posts` screen and create a new post.
- Add an `Image`, `Gallery`, `Video`, `Audio`, `File`, `Media Text` and `Cover` blocks to the post.
- Publish this newly created post.
- Verify that this newly created `Block Editor` related post of yours is being displayed as expected,
  both when previewing it from within the `Posts` and `Reader` screens. FYI: On `Reader`, you will
  find you post within the `FOLLOWING` tab.

</details>

<details>
    <summary>3. [JP] Reader Post Details [ReaderPostRenderer.java]</summary>

- Go to `Reader` screen and click on various posts.
- Verify that each and every post, along with all their details is being displayed as expected.

</details>

<details>
    <summary>4. [JP] Stats Insights - Latest Post Summary [LatestPostSummaryMapper.kt]</summary>

ℹ️ If the `Latest Post Summary` card is not being displayed, navigate to the bottom of the `Stats`
   screen and click on the `Add new stats card`. Then, enabled the `Latest Post Summary` from within
   the `Posts and Pages` group.

- Go to `Stats` screen and its `INSIGHTS` tab.
- Scroll to the `Latest Post Summary` card and verify that it is being displayed as expected.

</details>
