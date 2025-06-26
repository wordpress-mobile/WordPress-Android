# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

### Main Build Commands
- `./gradlew assembleWordPressVanillaDebug` - Build debug APK for WordPress app
- `./gradlew assembleJetpackVanillaDebug` - Build debug APK for Jetpack app  
- `./gradlew installWordPressVanillaDebug` - Install debug APK to connected device
- `./gradlew installJetpackVanillaDebug` - Install debug APK for Jetpack to device

### Testing Commands
- `./gradlew :WordPress:testWordPressVanillaDebugUnitTest` - Run unit tests for WordPress app
- `./gradlew :WordPress:connectedWordPressVanillaDebugAndroidTest` - Run instrumented tests for WordPress app
- `bundle exec fastlane build_and_run_instrumented_test app:wordpress` - Build and run WordPress instrumented tests in Firebase Test Lab
- `bundle exec fastlane build_and_run_instrumented_test app:jetpack` - Build and run Jetpack instrumented tests in Firebase Test Lab

### Code Quality Commands
- `./gradlew checkstyle` - Run Checkstyle linter (generates report in `WordPress/build/reports/checkstyle/checkstyle.html`)
- `./gradlew detekt` - Run Detekt linter for Kotlin (generates report in `WordPress/build/reports/detekt/detekt.html`)
- `./gradlew lintWordPressVanillaRelease` - Run Android lint on WordPress release variant

## Architecture Overview

### Multi-App Project Structure
This repository builds two apps from shared codebase:
- **WordPress** (`org.wordpress.android`) - Main WordPress mobile app
- **Jetpack** (`com.jetpack.android`) - Jetpack-branded version with feature differences

### Product Flavors and Build Types
- **App Flavors**: `wordpress`, `jetpack`
- **Build Type Flavors**: `vanilla` (release/beta), `wasabi` (development), `jalapeno` (CI/prototype)
- Common development variant: `jetpackWasabiDebug`

### Module Architecture
```
├── WordPress/                 # Main app module
├── libs/
│   ├── fluxc/                 # Networking and data layer (FluxC architecture)
│   ├── login/                 # Shared login functionality  
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
- **Line Length**: 120 characters max
- **No FIXME**: Use TODO instead of FIXME in committed code

### Development Workflow
- Default development flavor: `jetpackWasabi` (Jetpack app with beta suffix)
- Remote build cache available for faster builds (requires setup)
- Fastlane used for release automation and testing
- Secrets managed via `secrets.properties` file (not in repo)
- Pre-commit hooks may modify files during commit