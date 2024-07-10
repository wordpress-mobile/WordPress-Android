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

1. Plugin
    1. [Navigation](#navigation)
2. AndroidX Jetpack
    1. [WorkManager](#workmanager)
3. AndroidX/Google
    1. [Preference](#preference)
    2. [ExoPlayer](#exoplayer)
    3. [Webkit](#webkit)
4. Firebase/Google
    1. [Firebase](#firebase)
    2. [FirebaseIid](#firebaseiid)
    3. [MLKitBarcodeScanning](#mlkitbarcodescanning)
    4. [PlayServicesAuth](#playservicesauth)
    5. [PlayServicesCoreScanner](#playservicescodescanner)
    6. [PlayReview](#playreview)
5. Tool
    1. [Zendesk](#zendesk)
    2. [JSoup](#jsoup)
6. Other Core
    1. [AutoService](#autoservice)
    2. [KotlinPoet](#kotlinpoet)
7. Other UI
    1. [Lottie](#lottie)
    2. [UCrop](#ucrop)
8. [Smoke Test](#smoke-test)
9. [Special](#special)

ℹ️ Every test instruction should be prefixed with one of the following:
- [JP/WP] This test applies to both, the `Jetpack` and `WordPress` apps.
- [JP] This test applies to the `Jetpack` app only.
- [WP] This test applies to the `WordPress` app only.

-----

### Navigation [[navigationVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="navigation"></a>

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

-----

### WorkManager [[androidxWorkManagerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="workmanager"></a>

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

-----

### Preference (AndroidX) [[androidxPreferenceVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="preference"></a>

<details>
  <summary>1. [JP] Notifications Settings [NotificationsSettingsFragment.java]</summary>

- Go to `Notifications` tab.
- Click on the `Gear` setting button (top-right).
- Verify that the `Notification Settings` screen is displayed.
- Click on each of the settings within the `Notification Settings` settings screen and verify that
  every setting works as expected.

</details>

### Preference (Android) [[N/A](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)]

ℹ️ Note that this is not an actual dependency, it being part of the `Android` SDK, that it is
   deprecated and will be migrated to its `AndroidX` equivalent in the future. [[Issue](https://github.com/wordpress-mobile/WordPress-Android/issues/17962)]

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

-----

### ExoPlayer [[googleExoPlayerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="exoplayer"></a>

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

-----

### Webkit [[androidxWebkitVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="webkit"></a>

<details>
    <summary>1. [TODO] TODO</summary>

- TODO
- TODO
- TODO

</details>

-----

### Firebase [[firebaseBomVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="firebase"></a>

<details>
    <summary>1. [JP/WP] Push Notification on Post Being Published [GCMMessageService.java]</summary>

- Add a new `blog` post.
- Add any number of blocks.
- Publish the post.
- Verify that, on post being published, you get a push notification with the post's title, which,
  when you click on it, navigates you to the 'Notifications Detail' screen for this post.

</details>


-----

### FirebaseIid [[firebaseIidVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="firebaseiid"></a>

<details>
    <summary>1. [TODO] TODO</summary>

- TODO
- TODO
- TODO

</details>

-----

### MLKitBarcodeScanning [[googleMLKitBarcodeScanningVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="mlkitbarcodescanning"></a>


<details>
    <summary>1. [TODO] TODO</summary>

- TODO
- TODO
- TODO

</details>

-----

### PlayServicesAuth [[googlePlayServicesAuthVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="playservicesauth"></a>

ℹ️ These set of testing instructions are for internal contributors only, which can have access to
   upload an `.aab` to Google Play Store. As such, external contributors can't possibly test this
   update.

<details>
  <summary>1. [JP] Google Sign-in on Jetpack</summary>

ℹ️ Prerequisite: If you don't have access to upload an `.aab` to Google Play Store for
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

ℹ️ Prerequisite: If you don't have access to upload an `.aab` to Google Play Store for
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

-----

### PlayServicesCodeScanner [[googlePlayServicesCodeScannerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="playservicescodescanner"></a>

<details>
  <summary>1. [JP] Scan Login Code</summary>

Step.1:
- Build and install the `Jetpack` app (note that you don't need a release build, a debug build will
  suffice).
- Log in to the `Jetpack` app with a `WP.com` account (note that you need to use a non `A8C` account
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
- Follow the remaining prompts on your mobile to log in to WordPress on your web browser (desktop),
  verify that you have successfully logged-in and are able to use WordPress as expected.

</details>

-----

### PlayReview [[googlePlayReviewVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="playreview"></a>

<details>
    <summary>1. In app reviews</summary>

- Perform a clean install.
- Publish three (`AppReviewManager.TARGET_COUNT_POST_PUBLISHED + 1`) new posts or stories.
- Verify that there are no crashes.

</details>

-----

### Zendesk [[zendeskVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="zendesk"></a>

<details>
    <summary>1. [JP] Zendesk Update [Tickets & Contact Support]</summary>

ℹ️ This test only works when testing with a normal, non-a8c user account.

- Go to `Me` tab -> `Help` -> `Tickets`.
- Verify that the `My tickets` Zendesk related screen opens and that it lists all of your tickets
  (or non if you don't have any yet).
- Go back to the `Help` settings screen and then click on `Contact Support`.
- Verify that the `Help` Zendesk related screen opens and that you are prompt to `Send a message...`.
- Type `Testing Zendesk X.Y.Z update, please ignore and thank you!` and then send your message.
- After sending the first message, tap the `Contact support 📢` button to create the support ticket.
- Go back to the `Help` settings screen and then click on `Tickets` again.
- Verify that your previously sent message is listed in there and that you can click on it.
  PS: You could also check your emails and verify that you got a
  `Thank you for contacting the WordPress.com support team! (#1234567)` email.
- Verify that clicking on it navigates you to the inner screen where your message is shown as
  `Delivered` along with a predefined automated `mobile support` response message.

</details>

-----

### JSoup [[jsoupVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="jsoup"></a>

<details>
    <summary>1. [JP/WP] Classic Editor [Aztec]</summary>

ℹ️ Disable the `Block Editor` before testing this. You can do so by going to `Site Settings` ->
   `Editor` section -> `Use Block Editor` option and switch that off.
📝 Note that you might need to be on a business plan to have this `Editor` section available as not
   all sites have this option.

- Go to `Posts` screen and create a new post.
- Add an `Image`, `Video` and any other entry (like `Paragraph`, `Ordered List`, `Quote`, etc).
- Publish this newly created post.
- Verify that this newly created `Classic Editor` related post of yours is being displayed as
  expected, both when previewing it from within the `Posts` and `Reader` screens. FYI: On `Reader`,
  you will find you post within the `FOLLOWING` tab.

</details>

<details>
    <summary>2. [JP/WP] Block Editor [Gutenberg]</summary>

ℹ️ Enable the `Block Editor` before testing this. You can do so by going to `Site Settings` ->
`Editor` section -> `Use Block Editor` option and switch that on.
📝 Note that you might need to be on a business plan to have this `Editor` section available as not
all sites have this option.

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

-----

### AutoService [[googleAutoServiceVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="autoservice"></a>

<details>
    <summary>1. [JP/WP] Remote Features [@Feature]</summary>

️️ℹ️ Just test the most recent remote feature flag.

</details>

<details>
    <summary>2. [JP/WP] Features in Development [@FeatureInDevelopment]</summary>

ℹ️ Just test the most recent feature flag in development.

</details>

<details>
    <summary>3. [JP/WP] Remote Field Configs [@RemoteFieldDefaultGenerater]</summary>

ℹ️ Just test the most recent remote remote field configs.

</details>

<details>
    <summary>4. [JP/WP] Experimental Features [@Experiment]</summary>

ℹ️ Just test the most recent experimental feature.

</details>

-----

### KotlinPoet [[squareupKotlinPoetVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="kotlinpoet"></a>

<details>
    <summary>1. [JP/WP] Remote Features [@Feature]</summary>

️️ℹ️ Just test the most recent remote feature flag.

</details>

<details>
    <summary>2. [JP/WP] Features in Development [@FeatureInDevelopment]</summary>

ℹ️ Just test the most recent feature flag in development.

</details>

<details>
    <summary>3. [JP/WP] Remote Field Configs [@RemoteFieldDefaultGenerater]</summary>

ℹ️ Just test the most recent remote remote field configs.

</details>

<details>
    <summary>4. [JP/WP] Experimental Features [@Experiment]</summary>

ℹ️ Just test the most recent experimental feature.

</details>

-----

### Lottie [[lottieVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="lottie"></a>

<details>
    <summary>1. [JP/WP] JP Install Full Plugin Animation [JPInstallFullPluginAnimation.kt]</summary>

- TODO
- TODO
- TODO

</details>

<details>
    <summary>1. [WP] Jetpack Static Poster [JetpackStaticPoster.kt]</summary>

- TODO
- TODO
- TODO

</details>

<details>
    <summary>1. [JP] Notifications Screen [NotificationsDetailListFragment.kt]</summary>

- TODO
- TODO
- TODO

</details>

-----

### UCrop [[uCropVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="ucrop"></a>

<details>
    <summary>1. [JP/WP] Image Edit Screen [PreviewImageFragment.kt + CropFragment.kt]</summary>

- Add a new `blog` post.
- Add a new `image` block.
- Choose an image and wait for it to be uploaded within the `image` block.
- Click on the `media options` of this image (top right) and then click `edit`.
- Crop the image and click the `done` menu option (top right).
- Verify the image is updated accordingly.

</details>

-----

## Smoke Test <a name="smoke-test"></a>

1. Plugin
    1. [GoogleServices](#googleservices)
    2. [Dagger](#dagger)
2. Kotlin
    1. [Kotlin](#kotlin)
    2. [Coroutines](#coroutines)
3. EventBus
    1. [EventBus](#eventbus)
4. AndroidX Core
    1. [Annotation](#annotation)
    2. [ArchCore](#archcore)
    3. [Lifecycle](#lifecycle)
    4. [Core](#core)
    5. [Activity](#activity)
    6. [Fragment](#fragment)
    7. [AppCompat](#appcompat)
5. AndroidX Compose
    1. [Compose](#compose)
    2. [ComposeMaterial3](#composematerial3)
    3. [ComposeConstraintLayout](#composeconstraintlayout)
    4. [Coil](#coil)
6. AndroidX/Google
    1. [RecyclerView](#recyclerview)
    2. [ViewPager2](#viewpager2)
7. Material/Google
    1. [Material](#material)
    2. [ConstraintLayout](#constraintlayout)
8. Network
    1. [Retrofit](#retrofit)
    2. [Volley](#volley)
    3. [Glide](#glide)
    4. [Gson](#gson)
9. Tool
    1. [InstallReferrer](#installreferrer)
10. Other Core
    1. [ApacheCommons](#apachecommons)
11. Other
    1. [Desugar](#desugar)

ℹ️ Some smoke test instructions might have an `Extra` section with addition instructions.

-----

### GoogleServices [[googleServicesVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="googleservices"></a>

-----

### Dagger [[daggerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="dagger"></a>

-----

### Kotlin [[kotlinVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] + [[androidxComposeCompilerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="kotlin"></a>

-----

### Coroutines [[kotlinxCoroutinesVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="coroutines"></a>

-----

### EventBus [[eventBusVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="eventbus"></a>

-----

### Annotation [[androidxAnnotationVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="annotation"></a>

-----

### ArchCore [[androidxArchCoreVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="archcore"></a>

-----

### Lifecycle [[androidxLifecycleVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="lifecycle"></a>

-----

### Core [[androidxCoreVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="core"></a>

-----

### Activity [[androidxActivityVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="activity"></a>

-----

### Fragment [[androidxFragmentVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="fragment"></a>

-----

### AppCompat [[androidxAppcompatVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="appcompat"></a>

-----

### Compose [[androidxComposeBomVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="compose"></a>

-----

### ComposeMaterial3 [[androidxComposeMaterial3Version](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="composematerial3"></a>

-----

### ComposeConstraintLayout [[androidxConstraintlayoutComposeVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="composeconstraintlayout"></a>

-----

### Coil [[coilComposeVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="coil"></a>

-----

### RecyclerView [[androidxRecyclerviewVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="recyclerview"></a>

-----

### ViewPager2 [[androidxViewpager2Version](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="viewpager2"></a>

-----

### Material [[googleMaterialVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="material"></a>

-----

### ConstraintLayout [[androidxConstraintlayoutVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="constraintlayout"></a>

-----

### Retrofit [[squareupRetrofitVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="retrofit"></a>

-----

### Volley [[androidVolleyVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="volley"></a>

-----

### Glide [[glideVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] + [[glideVolleyVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="glide"></a>

-----

### Gson [[googleGsonVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="gson"></a>

-----

### InstallReferrer [[androidInstallReferrerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="installreferrer"></a>

-----

### ApacheCommons [[apacheCommonsTextVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="apachecommons"></a>

-----

### Desugar [[androidDesugarVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="desugar"></a>

<details>
    <summary>Extra</summary>

Focus on testing push notifications and/or blogging reminders since this was why this library was
introduced to this project in the first place (see [here](https://github.com/wordpress-mobile/WordPress-Android/pull/14698)
and [here](https://github.com/orgs/wordpress-mobile/projects/95)).

</details>

-----

## Special <a name="special"></a>

1. Gradle
    1. [Gradle](#gradle)
2. Plugin
    1. [AGP](#agp)
    2. [Sentry](#sentry)
    3. [Detekt](#detekt)
    4. [ViolationComments](#violationcomments)
3. React Native
    1. [ReactNative](#reactnative)

ℹ️ Every special test instructions have a `Why & Extra` section to further explain why that
   dependency update is so special and how to deal with it.

-----

### Gradle [[gradleWrapper](https://github.com/wordpress-mobile/WordPress-Android/tree/trunk/gradle/wrapper)] <a name="gradle"></a>

<details>
    <summary>Why & How</summary>

- TODO
- TODO
- TODO

</details>

-----

### AGP [[agpVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="agp"></a>

<details>
    <summary>Why & How</summary>

- TODO
- TODO
- TODO

</details>

-----

### Sentry [[sentryVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="sentry"></a>

<details>
    <summary>Why & How</summary>

`sentryVersion` in this project relates to Sentry Gradle Plugin only. Sentry SDK is bundled with
[Automattic-Tracks-Android](https://github.com/Automattic/Automattic-Tracks-Android).

#### Why?
We use Sentry Gradle Plugin to send ProGuard mapping files and source context files to Sentry. It
makes stacktrace readable on Sentry dashboard. This should be the main focus when testing after
bumping `sentryVersion`.

#### To Test

Please build the release variant (`vanillaRelease`) of both WordPress and Jetpack flavors and verify if issues are sent correctly. You can use the following snippet.

<details><summary>PATCH (warning: it'll probably have some conflicts in the future when `WPMainActivityViewModel` change. It's more for an idea:</summary>

```PATCH
Subject: [PATCH] tests: add a test for features in development generation
---
Index: WordPress/src/main/java/org/wordpress/android/viewmodel/main/WPMainActivityViewModel.kt
IDEA additional info:
Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
<+>UTF-8
===================================================================
diff --git a/WordPress/src/main/java/org/wordpress/android/viewmodel/main/WPMainActivityViewModel.kt b/WordPress/src/main/java/org/wordpress/android/viewmodel/main/WPMainActivityViewModel.kt
--- a/WordPress/src/main/java/org/wordpress/android/viewmodel/main/WPMainActivityViewModel.kt	(revision 806913d9fb807250cecd5b24b36001d55ea4c255)
+++ b/WordPress/src/main/java/org/wordpress/android/viewmodel/main/WPMainActivityViewModel.kt	(date 1710772966823)
@@ -5,6 +5,7 @@
 import androidx.lifecycle.LiveData
 import androidx.lifecycle.MutableLiveData
 import androidx.lifecycle.distinctUntilChanged
+import com.automattic.android.tracks.crashlogging.CrashLogging
 import kotlinx.coroutines.CoroutineDispatcher
 import kotlinx.coroutines.flow.firstOrNull
 import org.wordpress.android.R
@@ -67,6 +68,7 @@
     private val bloggingPromptsStore: BloggingPromptsStore,
     @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
     private val shouldAskPrivacyConsent: ShouldAskPrivacyConsent,
+    private val crashLogging: CrashLogging,
 ) : ScopedViewModel(mainDispatcher) {
     private var isStarted = false

@@ -161,6 +163,7 @@
         launch { loadMainActions(site) }

         updateFeatureAnnouncements()
+        crashLogging.sendReport(Throwable("Test crash"))
     }

     @Suppress("LongMethod")
```
</details>


</details>

-----

### Detekt [[detektVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="detekt"></a>

<details>
    <summary>Why & How</summary>

- TODO
- TODO
- TODO

</details>

-----

### ViolationComments [[violationCommentsVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/settings.gradle)] <a name="violationcomments"></a>

<details>
    <summary>Why & How</summary>

- TODO
- TODO
- TODO

</details>

-----

### ReactNative [[facebookReactVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle)] <a name="reactnative"></a>

<details>
    <summary>Why & How</summary>

- TODO
- TODO
- TODO

</details>
