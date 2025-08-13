#  WordPress for Android

[![Build Status](https://img.shields.io/github/actions/workflow/status/wordpress-mobile/WordPress-Android/ci.yml?branch=trunk)](https://github.com/wordpress-mobile/WordPress-Android/actions)
[![License: GPL v2](https://img.shields.io/badge/License-GPL_v2-blue.svg)](LICENSE.md)
[![Contributors](https://img.shields.io/github/contributors/wordpress-mobile/WordPress-Android)](https://github.com/wordpress-mobile/WordPress-Android/graphs/contributors)
[![Google Play](https://img.shields.io/badge/Get_it_on-Google_Play-green.svg)](https://play.google.com/store/apps/details?id=org.wordpress.android)

> The official **WordPress** app for Android — publish posts, check stats, and manage your site from anywhere.  
>  **Install**: https://play.google.com/store/apps/details?id=org.wordpress.android  
>  **Build from source**: follow the guide below.

---

##  Build Instructions

1) Install **Android Studio**: https://developer.android.com/studio  
2) Install **npm** via **nvm** (see step 1 of the Block Editor Quickstart): https://developer.wordpress.org/block-editor/getting-started/devenv/#quickstart  
3) Clone the repo and open it in Android Studio (this will generate `local.properties`):

    git clone https://github.com/wordpress-mobile/WordPress-Android.git
    cd WordPress-Android

4) **JDK 11 recommended**: set `JAVA_HOME` and Android Studio’s JDK to 11 for best CI/test compatibility.  
5) Create an emulator: **Tools → AVD Manager**.  
6) Hit **Run**.

**Note:** Ignore the Gradle plugin update prompt unless you plan to handle compatibility fixes.

---

##  Build & Test (CLI)

    # Build debug APK
    ./gradlew assembleWordPressVanillaDebug

    # Install debug APK to connected device/emulator
    ./gradlew installWordPressVanillaDebug

    # Run unit tests
    ./gradlew :WordPress:testWordPressVanillaDebugUnitTest

    # Run instrumented Android tests
    ./gradlew :WordPress:connectedWordPressVanillaDebugAndroidTest

---

## 📲 Running the App

You can test using your own WordPress site or a free temporary site from **https://jurassic.ninja**.

**Steps**  
1. Launch the app.  
2. Tap **“Enter your existing site address”**.  
3. Enter your site URL and credentials.

**Note:** WordPress.com features are disabled in development builds.

---

##  Directory Structure

    .
    ├── libs/                    # Debug variant dependencies
    ├── tools/                   # Helper scripts
    ├── gradle.properties        # Build script properties
    ├── WordPress/
    │   ├── build.gradle         # Main module build script
    │   └── src/
    │       ├── main/            # App source code, assets, resources
    │       ├── test/            # Unit tests
    │       ├── androidTest/     # Instrumented tests
    │       ├── debug/           # Debug variant
    │       └── wasabi/          # Wasabi variant-specific files

---

## 🔑 Google Configuration

- Google Sign-In works **only** for WordPress.com accounts in the **official Play Store** build.  
- Development builds **will fail** Google Sign-In (private config files are not included).  
- Learn more: https://developers.google.com/identity/

---

## 🤝 Contributing

See **CONTRIBUTING.md** for details on:
- Reporting issues  
- Submitting pull requests  
- Coding style & PR guidelines

**Quick workflow**

    # Create a feature branch
    git checkout -b feature/short-description

    # Make focused commits, run tests, push, open a PR

**Commit message tips**
- Use present tense: `Fix crash when opening editor without network`  
- Reference issues: `Fixes #1234`

**Pull request tips**
- Keep PRs small & focused  
- Describe the change, rationale, and user impact  
- Include test notes and screenshots (for UI changes)  
- Ensure CI checks pass

---

## 📜 Code of Conduct

This project follows the **Contributor Covenant** (v2.1).  
Be respectful, inclusive, and constructive. No harassment or hate speech.  
Full text: https://www.contributor-covenant.org/version/2/1/code_of_conduct/  
Community chat: https://chat.wordpress.org (`#mobile`)

---

## 🛡 Security

Please **do not** open public issues for security problems.  
Report privately via **HackerOne (Automattic)**: https://hackerone.com/automattic

---

## 📚 Documentation

- Coding Style: `docs/coding-style.md`  
- Pull Request Guidelines: `docs/pull-request-guidelines.md`  
- More docs in `docs/`

---

## 🌐 Resources

- WordPress Mobile Blog: http://make.wordpress.org/mobile  
- WordPress Mobile Handbook: http://make.wordpress.org/mobile/handbook/

---

## 📄 License (GPL-2.0)

WordPress for Android is licensed under the **GNU General Public License v2**.  
Some code in `libs/` may be under separate, GPL-compatible licenses.

    WordPress for Android
    Copyright (C) 2025 Automattic

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License, version 2.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

Full license: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
