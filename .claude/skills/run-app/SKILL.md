---
name: run-app
description: >
  Build and run the Jetpack Android app on a device or emulator.
  This skill should be used when the user asks to run the app,
  launch the app, or invokes /run-app.
---

# Run App

Build and run the Jetpack wasabi debug variant on a connected Android
device or emulator. By default this skill builds and runs the
**Jetpack** app. Only build the WordPress app if the user explicitly
asks for it.

## Steps

### 1. Build the app

By default, build the Jetpack wasabi debug variant:

```bash
./gradlew assembleJetpackWasabiDebug
```

If the user explicitly asks for the WordPress app instead, run:

```bash
./gradlew assembleWordPressWasabiDebug
```

### 2. Check for connected devices

List all connected/running devices and emulators:

```bash
adb devices -l
```

Parse the output to identify running devices. Ignore the header line
and any lines that do not contain a device serial.

### 3. Determine which device to target

- **One device connected**: use it automatically.
- **Multiple devices connected**: present the list to the user with
  `AskUserQuestion` and let them pick which device to use.
- **No devices connected**: go to step 4.

### 4. Handle no connected devices

List available (offline) AVDs:

```bash
emulator -list-avds
```

- **AVDs available**: present the list to the user with
  `AskUserQuestion` and let them pick one. Before starting the
  emulator, snapshot the current device list so you can identify
  the new serial afterwards:

  ```bash
  adb devices -l          # snapshot before
  emulator -avd <avd_name> &
  adb wait-for-device
  ```

  After `wait-for-device` returns, poll until the device has
  finished booting:

  ```bash
  adb [-s <serial>] shell getprop sys.boot_completed
  ```

  Repeat every 2 seconds until the output is `1`. Then run
  `adb devices -l` again and diff against the earlier snapshot to
  resolve the new emulator's serial for subsequent `-s` flags.

- **No AVDs available**: warn the user that no devices or emulators
  are available and suggest they connect a device or create an AVD
  through Android Studio.

### 5. Install and launch the app

Find the built APK dynamically (the exact filename may change across
Gradle versions):

**Jetpack (default):**

```bash
APK=$(find WordPress/build/outputs/apk/jetpackWasabi/debug \
  -name '*.apk' ! -name '*androidTest*' | head -1)
```

**WordPress (only if the user explicitly requested it):**

```bash
APK=$(find WordPress/build/outputs/apk/wordpressWasabi/debug \
  -name '*.apk' ! -name '*androidTest*' | head -1)
```

Install the APK on the target device (use `-s <serial>` when multiple
devices are present):

```bash
adb [-s <serial>] install -r "$APK"
```

Then launch the app:

- **Jetpack**: `adb [-s <serial>] shell am start -n com.jetpack.android.beta/org.wordpress.android.ui.WPLaunchActivity`
- **WordPress**: `adb [-s <serial>] shell am start -n org.wordpress.android.beta/org.wordpress.android.ui.WPLaunchActivity`

### 6. Report the result

Tell the user whether the app was successfully installed and launched,
or report any errors encountered during the process.

## Important Rules

- Always build before installing to ensure the APK is up to date.
- When multiple devices are connected, NEVER pick one silently —
  always ask the user.
- When starting an emulator, run it in the background so it does not
  block the agent.
- If the build fails, report the error and do NOT attempt to install
  a stale APK.
