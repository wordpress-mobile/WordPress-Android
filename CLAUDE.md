# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

### Main Build Commands
- `./gradlew assembleWordPressDebug` - Build debug APK for WordPress app
- `./gradlew assembleJetpackDebug` - Build debug APK for Jetpack app
- `./gradlew installWordPressDebug` - Install debug APK to connected device
- `./gradlew installJetpackDebug` - Install debug APK for Jetpack to device

### Testing Commands
- `./gradlew :WordPress:testWordPressDebugUnitTest` - Run unit tests for WordPress app
- `./gradlew :WordPress:connectedWordPressDebugAndroidTest` - Run instrumented tests for WordPress app
- `bundle exec fastlane build_and_run_instrumented_test app:wordpress` - Build and run WordPress instrumented tests in Firebase Test Lab
- `bundle exec fastlane build_and_run_instrumented_test app:jetpack` - Build and run Jetpack instrumented tests in Firebase Test Lab

### Code Quality Commands
- `./gradlew checkstyle` - Run Checkstyle linter (generates report in `WordPress/build/reports/checkstyle/checkstyle.html`)
- `./gradlew detekt` - Run Detekt linter for Kotlin (generates report in `WordPress/build/reports/detekt/detekt.html`)
- `./gradlew lintWordPressRelease` - Run Android lint on WordPress release variant

## Architecture Overview

### Multi-App Project Structure
This repository builds two apps from shared codebase:
- **WordPress** (`org.wordpress.android`) - Main WordPress mobile app
- **Jetpack** (`com.jetpack.android`) - Jetpack-branded version with feature differences

### Product Flavors and Build Types
- **App Flavors**: `wordpress`, `jetpack`
- **Build Types**: `debug`, `release`
- Common development variant: `jetpackDebug`

### Module Architecture
```
├── WordPress/                 # Main app module
├── libs/
│   ├── fluxc/                 # Networking and data layer (FluxC architecture)
│   ├── editor/                # Block editor integration
│   ├── image-editor/          # Image editing functionality
│   ├── analytics/             # Analytics and tracking
│   ├── networking/            # Network utilities
│   └── processors/            # Annotation processors
```

### Key Architectural Patterns
- **FluxC**: Unidirectional data flow architecture (like Redux)
  - Actions → Dispatcher → Stores → Views
  - Located in `libs/fluxc/` module
- **MVVM**: ViewModels with LiveData for UI components
- **Dependency Injection**: Dagger Hilt for DI container
- **Jetpack Compose**: Modern UI toolkit for newer screens
- **View Binding**: For traditional XML layouts

### Core Feature Areas
```
WordPress/src/main/java/org/wordpress/android/
├── ui/                        # UI layer organized by feature
│   ├── posts/                # Post creation and management
│   ├── reader/               # Content discovery and reading
│   ├── stats/                # Site analytics and statistics
│   ├── bloggingreminders/    # Posting reminders system
│   ├── comments/             # Comment management
│   ├── accounts/             # Authentication and signup
│   ├── domains/              # Domain management
│   └── deeplinks/            # Deep link handling
├── models/                   # Data models and DTOs
├── util/                     # Shared utilities and helpers
├── networking/               # Network layer components
└── modules/                  # Dagger dependency injection modules
```

### Build Configuration Details
- Uses Gradle Version Catalog for dependency management (`gradle/libs.versions.toml`)

### Dependencies
- Add new dependencies to `gradle/libs.versions.toml` (version catalog)
- Never add dependencies directly in build.gradle files
- Check for existing similar dependencies before adding new ones

### UI Development
- Prefer Jetpack Compose for new screens
- Use View Binding for existing XML layouts (not findViewById)
- Follow Material Design 3 guidelines

### Testing Strategy
- **Unit Tests**: Located in `src/test/` using JUnit, Mockito, AssertJ
- **Instrumented Tests**: Located in `src/androidTest/` using Espresso
- **UI Tests**: Can be run locally or on Firebase Test Lab via Fladle plugin
- **Excluded Test Packages**: `org.wordpress.android.ui.screenshots` (for CI optimization)

### Code Quality Tools
- Android Code Style Guidelines with project-specific Checkstyle and Detekt rules
- **Checkstyle**: Java code style enforcement (`config/checkstyle.xml`)
- **Detekt**: Kotlin code analysis (`config/detekt/detekt.yml`)
- **Android Lint**: Built-in Android static analysis
- **IMPORTANT**: Do NOT run static analysis tools (checkstyle, detekt, lint) automatically during
  coding. Only run them when explicitly requested or when working on a static analysis-related task.
  This saves processing time.
- **Line Length**: 120 characters max - **ALWAYS respect this limit in all code, comments, and strings**
- **No FIXME**: Use TODO instead of FIXME in committed code
- **No Deprecated APIs**: Avoid using deprecated methods and classes in new code
- **No Reflection**: Avoid using reflection in new code; prefer type-safe alternatives

### Code Style Guidelines
- **IMPORTANT**: Always keep lines at or under 120 characters
- Break long lines by:
  - Splitting long comments across multiple lines
  - Breaking method chains at logical points
  - Splitting long strings with string concatenation or multiline strings
  - Breaking parameter lists across multiple lines
  - Using proper indentation for continuation lines

### Kotlin Conventions
- **Kotlin First**: All new classes must be written in Kotlin; avoid Java for new code
- **No Empty Lines After Braces**: Do not add empty lines immediately after opening braces (`{`)
- **Companion Object Placement**: Place `companion object` at the bottom of the class, after all
  other members (properties, init blocks, constructors, functions)

### Development Workflow
- Default development variant: `jetpackDebug`
- Remote build cache available for faster builds (requires setup)
- Fastlane used for release automation and testing
- Secrets managed via `secrets.properties`, decrypted outside the checkout by `a8c-secrets decrypt`
- Pre-commit hooks may modify files during commit

### Git Operations Checklist
**IMPORTANT**: Claude must review and confirm ALL items in this checklist before performing any git commit or push operation.
- [ ] Have you explicitly asked the user for permission for THIS specific operation?
- [ ] Did the user respond affirmatively to THIS specific request (not a previous one)?
- [ ] Are you committing only the changes the user approved?

### Editing PR Descriptions
**IMPORTANT**: When asked to modify a PR description, always first retrieve the current
description from the server (e.g. `gh pr view <number> --json body`) and edit that live
content. Never rewrite the whole description from local knowledge or memory, since it may
have been changed on the server side and those changes would be lost.

## User-Facing Changes Log

`docs/USER-FACING-CHANGES.md` is a rolling log of changes a customer could notice, written by the author of the change. It is not release notes. `docs/USER-FACING-CHANGES-DOCUMENTATION.md` covers what belongs in the log and the entry format.

When a change could be noticed by a customer, read the documentation file and add an entry at the top of the log as part of the same change.
