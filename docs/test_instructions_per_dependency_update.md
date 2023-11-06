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
