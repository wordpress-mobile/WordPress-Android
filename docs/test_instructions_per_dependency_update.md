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
