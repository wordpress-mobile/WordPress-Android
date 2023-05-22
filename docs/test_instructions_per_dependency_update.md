# Test Instructions per Dependency Update

## Purpose

The purpose of this document is to establish a set of guidelines for testing the app during
dependency updates. Specifically, it outlines a series of test instructions that should be adhered
to when updating a dependency.

## Content

1. [WorkManager](#workmanager)
2. [Preference](#preference)
3. [Next](#next)

### WorkManager [[androidxWorkManagerVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L54)] <a name="workmanager"></a>

<details>
  <summary>1. UploadWorker.kt</summary>

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
  <summary>2. ReminderWorker.kt</summary>

- Go to `Site Settings` screen.
- Find the `Blogging` section, click on `Reminders`, toggle-on every day and click on `Update`.
- Notice the `All set!` bottom sheet appearing, click `Done`.
- Close the app, preferably swipe the app off.
- Go to the device's `Settings` app, find the `Date & Time` section, turn `Automatic date & time` off.
- Set the device's date to a day after today.
- Open the app.
- Verify the `Blogging Reminders` notification appearing. For example, the notification title could be `Daily Prompt`, while the notification description something like `Is there anything you feel too old to do anymore?`.

</details>

### Preference [[androidxPreferenceVersion](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L50)] <a name="preference"></a>

<details>
  <summary>1. AccountSettingsFragment.kt</summary>

- Go to `Me` screen.
- Click on the `Account Settings` button.
- Verify that the `Account Settings` screen is displayed.
- Click on each of the settings within the `Account Settings` screen and verify that every setting works as expected.

</details>

<details>
  <summary>2. AppSettingsFragment.java</summary>

- Go to `Me` screen.
- Click on the `App Settings` button.
- Verify that the `App Settings` screen is displayed.
- Click on each of the settings within the `App Settings` screen and verify that every setting works as expected, including the inner settings like the `Privacy Settings` and `Debug Settings` screens.

</details>

<details>
  <summary>3. SiteSettingsFragment.java</summary>

- While on the `My Site/MENU` tab.
- Click on the `Site Settings` button.
- Verify that the `Site Settings` screen is displayed.
- Click on each of the settings within the `Site Settings` screen and verify that every setting works as expected.

</details>

<details>
  <summary>4. JetpackSecuritySettingsFragment.java</summary>

- Prerequisite: You must have a Jetpack connected site, which displays this setting.
- While on the `My Site/MENU` tab.
- Click on the `Jetpack Settings` button.
- Verify that the `Security` setting screen is displayed.
- Click on each of the settings within the `Security` settings screen and verify that every setting works as expected.

</details>

<details>
  <summary>5. NotificationsSettingsFragment.java</summary>

- While on the `Notifications` tab.
- Click on the `Gear` setting button (top-right).
- Verify that the `Notification Settings` screen is displayed.
- Click on each of the settings within the `Notification Settings` settings screen and verify that every setting works as expected.

</details>

<details>
  <summary>6. EditPostActivity.java</summary>

- Go to `Post` screen.
- Edit a new post, which uses `PreferenceManager` to `setDefaultValues(...)` for `Account Settings`, add a few of the main blocks and verify that everything is workings as expected.

</details>

<details>
  <summary>7. MySitesPage.java</summary>

- Run the `StatsTests` UI test suite, which uses the `MySitesPage.java` class, and verify that all tests pass.

</details>

### Next [[next](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/build.gradle#L1)] <a name="next"></a>

TODO
