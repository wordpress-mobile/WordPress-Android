# How to generate & update Play Store Screenshots

The screenshots used for the Play Store come in multiple device sizes (phone vs tablet) and many locales (currently 16 different languages), so it would be unreasonable to expect designers to generate all the variants by hand every time we want to update those screenshots. For this reason, the generation of those Play Store screenshots is automated with Fastlane.

## High Level overview

Generating new screenshots for the Play Store consists of following this multi-stage process:

 - First, we have UI tests in the codebase — namely [`WPScreenshotTest`](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/WordPress/src/androidTest/java/org/wordpress/android/ui/screenshots/WPScreenshotTest.java) and [`JPScreenshotTest`](https://github.com/wordpress-mobile/WordPress-Android/blob/trunk/WordPress/src/androidTest/java/org/wordpress/android/ui/screenshots/JPScreenshotTest.java) — which are responsible for launching the app, navigating through the various screens we need screenshot for, and taking a "raw screenshot" (screen capture) of those screens.
    - If you plan to update the Play Store screenshots to include different screens that the ones currently included, or if the navigation of the app to get to those screens changed, you'll need to update those `WPScreenshotTest` and `JPScreenshotTest` test suites accordingly.
 - Then, the lane `screenshots` is responsible for:
    - Creating dedicated emulators with the right device model and API level we need for taking the screenshots.
    - Then run the `WPScreenshotTest` / `JPScreenshotTest` UI test in all the supported locales (currently 16), to generate "raw screenshots" in `fastlane/screenshots/{app}/raw/` for all the captured screens, in each of the locales, and for each of the device models / emulators (typically, one `phone` model, and one `tenInch` tablet model).
 - Once we have those "raw screenshots" captured from the app screens in all the locales, the lane `create_promo_screenshots` assembles (doing image composition, using `imagemagick`) those raw screenshots in a background image and adding a device frame + some (localized) annotation text from the `screenshot_{N}.txt` files. The way to assemble those elements (paths to the files, coordinates where to place those in final image, etc.) is described in the `fastlane/screenshots/{app}-config.json` file.

> **Vocabulary**
> - We call "raw screenshots" the images obtained by capturing the various screens of the app (as they appear on the emulator) while navigating in the app using the dedicated UI tests.
> - We call "promo screenshots" the final images obtained after compositing, i.e. with those raw screenshots integrated with some background image, device frame, and promotional text around it, ready to be used as images for the PlayStore.

## Raw Screenshots

### How to update the list of screens of the app being captured?

If you need to capture different raw screenshots of the app' screens (to include in the final promo screenshots), you'll need to update the `WPScreenshotTest.java` / `JPScreenshotTest.java` accordingly.

If the navigation of the app evolved since last time you ran those screenshot tests, and the way to get to those screens changed, you'll obviously also need to update the UI tests accordingly.

When working on the UI Tests code, use `takeScreenshot(file_name)` to trigger a screenshot of the emulator's screen at that point in the test and save it with that file name. (This file name will then be referenced in the `{app}-config.json` file for the promo screenshots to insert that raw screenshot in the final image.)

> **Warning**: When testing your updates of the `WPScreenshotTest` / `JPScreenshotTest` UI Tests, be sure to validate that they don't make any assumption on the locale in which they'll be run in. In particular, make sure you don't add UI expectations on elements based on their text, because in other locales that text would be translated, thus different.

> **Warning**: While you can run your UI Tests in Android Studio to validate that they work and do what you expect, especially while you do multiple iterations to make them work, remember to (1) ideally run those in the same emulators that `fastlane screenshots` will ultimately use, to avoid any surprise, and that (2) you will have to run `fastlane screenshots` at some point to validate that they also work in other locales and not just the default English locale.


### How to generate the raw screenshots from your UI Tests?

To generate all the raw screenshots in all the supported locales and for all the device models we generate Play Store screenshots for, run the following commands:
 
```
# Generate raw screenshots for the WordPress app, in all locales and device models we support
bundle exec fastlane screenshots app:wordpress

# Generate raw screenshots for the Jetpack app, in all locales and device models we support
bundle exec fastlane screenshots app:jetpack
```

If you want to test your screenshots just in a specific locale and for only one type of device — typically to validate that they will work in locales other than English while you're still iterating and validating your UI test changes — you can use the following:

```
# Replace `${app}` with `wordpress` or `jetpack`
# Replace `${deviceType}` with `phone` or `tenInch`
# Replace `${localeCode}` with the locale code for which you want to generate the screenshots, e.g. `fr-FR`

bundle exec fastlane screenshot app:${app} device:${deviceType} locale:${localeCode}
```

Then you can validate that your UI tests pass, and that the corresponding `fastlane/{metadata,jetpack_metadata}/android/${localeCode}/images/${deviceType}/*.png` files look as expected

### How to update the emulators used for capturing the raw screenshots?

The device model and API level of the emulators that will be used for running the UI Tests and capturing the raw screenshots are defined by the `SCREENSHOT_DEVICES` constant in `fastlane/lanes/screenshots.rb`.

You can modify the value of `device:` and `api:` to update the device model and API Level the lane will use when it will created the dedicated emulators before running the tests to capture the screenshots.

## Promo Screenshots

### Configuration File

The instructions on how to assemble the various parts (background, raw screenshot, device frame, promotional text) of the final promo screenshots are defined in the `fastlane/screenshots/{app}-config.json` files.

The format and expected keys/structure for this config file are documented in details in [this doc on the release-toolkit repo](https://github.com/wordpress-mobile/release-toolkit/blob/trunk/docs/screenshot-compositor.md#creating-a-configuration-file).

💡 Some things to keep in mind:

 - If you updated the device models (`device:` key of `SCREENSHOT_DEVICES` constant, see above) used for the raw screenshots, you will probably want to:
    - Update the device frames images used in this config file, to match the look and size of the new devices you chose.
    - Update all the coordinates used in the config file, as the position and size of the raw screenshots might have changed with the new device model you chose if that model has a different screen size.
 - ⚠️ Before generating the promo screenshots, be sure to install the custom font(s) that the `.css` stylesheets, referenced in this config file, are using.
    - The font(s) to install can be found in `fastlane/screenshots/assets/fonts`
    - Since the font used for the promotional text is defined by CSS, if the file is missing, the HTML renderer used to draw the text on the final promo screenshots will just fallback to a default font if the one defined in the CSS is not installed / not found in your system. So that can be an easy thing to miss.

### Translating the promotional texts

The promotional texts that you want to add to your screenshots will have to be translated / localized just like any other metadata used on the Play Store in different locales.
This will usually take a full sprint (to give time for polyglots to translate them all), so be sure to take this into account in your planning.

To do this:

 - Update the `screenshot_{n}.txt` files in `WordPress/{metadata,jetpack_metadata}/` with the new source English copies that you need to get translated.
 - Wait for the release manager to do the next code freeze, which will include the new content of those `.txt` files into the `PlayStoreStrings.po` file and then will be imported into GlotPress for translation (same process than for the release notes translation)
 - During the beta test phase that runs post code freeze, polyglots will then translate your new copies into the various locales in GlotPress.
 - At the end of the sprint, once all the translations have been done and retrieved from GlotPress as part of the release finalization, you should then have `fastlane/{jetpack_,}metadata/android/{locale}/screenshot_{n}.txt` files translated in each of the locales, ready to be referenced by your config file and used for the promo screenshots generation in all the locales.

If you need a faster feedback loop or to accelerate the translation, get in touch with your Release Manager and/or with Team Global, to see if we can fasttrack the translations outside of the regular cycle; though polyglots will still always need time to do translations, so keep in mind that this will never be instantaneous and you need to account for it in your planning about updating PlayStore screenshots.

### Installing `imagemagick`, `rmagick` and `drawText`

To do the image compositing, we rely on the ImageMagick library and the Ruby wrapper gem `rmagick` that interfaces it.
We also use [the `drawText` tool that we wrote](https://github.com/automattic/drawText) (and whose brew formula is hosted internally [here](https://github.com/Automattic/homebrew-build-tools)) to draw styled HTML text on those final images.

This means that you will need to install those on your machine first before being able to run the lane generating the promo screenshots:

 - Run `brew install imagemagick` to install the ImageMagick library on your system.
 - Run `brew install automattic/build-tools/drawText` to install the `drawText` executable.
 - Then run `bundle install --with screenshots` to include the `rmagick` gem in the list of gems installed.
    - This will make a change to the `.bundle/config` file of the repository. Please don't commit that change, so that other developers won't require to have `rmagick` installed for everything else they work on — as this library and gem is only needed for generating screenshots, which is not as common as all the other everyday tasks developers run.
 
> **Note**: This (especially `brew install imagemagick` and `brew install automattic/build-tools/drawText`) requires you to install things on you machine _system-wide_ (`brew install` install things at the machine level, for all users, unlike `bundle` which installs things only in the folder of your current repo / working copy). So this is a bit invasive.
>
> This also sadly comes with risks of having installation troubles depending on the environment of your machine (and which libraries and headers you have installed, etc); in fact, it's common to have issues when trying to `brew install imagemagick` or to install `rmagick`, especially on the steps which tries to compile those libraries (since they rely on compiled, binary code and libraries) and might come up with compilation errors or missing headers and the like. There's sadly no magic silver bullet solution for those, and search engines and StackOverflow are usually your best friends here.

### Generating the promo screenshots

Once your `{app}-config.json` config file is properly setup, and you ensured you've installed the custom font(s) used by the `.css` file(s) used by your config file, you can trigger the compositing process for generating all the promo screenshots in all locales and device models using:

```
# Generate all promo screenshots for WordPress, based on the already-generated raw screenshots and the config file from previous step
bundle exec fastlane create_promo_screenshots` app:wordpress

# Generate all promo screenshots for Jetpack, based on the already-generated raw screenshots and the config file from previous step
bundle exec fastlane create_promo_screenshots` app:jetpack
```

### Uploading the promo screenshots to the Play Store

This last step is usually done by the Release Manager, in parallel to when they submit of a new app version to the Play Store.

The lane to use to automate the upload of all the localized promo screenshots to Play Store—and replace the existing screenshots with the new ones—is:

```
bundle exec fastlane upload_and_replace_screenshots_in_play_store
```
