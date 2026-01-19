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
- Default development flavor: `jetpackWasabi` (Jetpack app with beta suffix)
- Remote build cache available for faster builds (requires setup)
- Fastlane used for release automation and testing
- Secrets managed via `secrets.properties` file (not in repo)
- Pre-commit hooks may modify files during commit

### Git Operations Checklist
**IMPORTANT**: Claude must review and confirm ALL items in this checklist before performing any git commit or push operation.
- [ ] Have you explicitly asked the user for permission for THIS specific operation?
- [ ] Did the user respond affirmatively to THIS specific request (not a previous one)?
- [ ] Are you committing only the changes the user approved?

## Release Notes Compilation Process

### Overview
Process for compiling release notes for new versions, ensuring they meet Play Store character limits (under 500, preferably 350-400) and maintain the established playful tone.

### Step-by-Step Process

#### 1. Study Previous Release Notes Style
Use `gh` to fetch releases and analyze professional editorialization patterns:
- **Version 25.8**: Raw added in commits `8cd1b4cc268887b9e2de94fb19afc108b011f0da` & `93591923dd9a9c144729952ad0dfbc9306f69894`, editorialized in `8a7fc316cf0acfc2b88b1090e3ca148f2145fa92`
- **Version 25.9**: Raw added in `b4a292cc9c9a30823bfdf8e770479a68362ed500`, editorialized in `dad8fa582856758999c43384ccbe55f34b6d2e17`
- **Version 26.0**: Raw added in `a53234ea6b13adf18acac262ad985f94308191c6` & `63067fb7249cc860d44547bb0d1b8dee975dbe8e`, editorialized in `02854d67c42146e611f0f0dff9feb0fd48dc7fd8`

Commands: `gh release view 25.8`, `gh release view 25.9`, `gh release view 26.0` (note: no 'v' prefix)

#### 2. Verify Release Branch and Get Last Release Hash
- Verify current branch follows naming: `release/x.y` (where x.y = last_release + 0.1)
- Get commit hash for last release: `gh release view <last_version> --json tagName,targetCommitish`
- Confirm current branch is properly ahead of last release tag

#### 3. Identify Changes Since Last Release
Compare current release branch against last release hash:
```bash
git log <last_release_hash>..HEAD --oneline --no-merges
```
Focus on user-facing changes from squash commit messages. **Important**: When commit messages are unclear or technical, always investigate further:
- Use `gh pr view <PR_number>` to read PR titles and descriptions
- Look for keywords indicating user-facing changes: "feat:", new functionality, UI changes, user experience
- Be especially careful with feature rollouts that may have technical-sounding commit messages but represent new user functionality
- When in doubt, investigate the PR rather than excluding potentially important features

#### 4. Compile Raw Release Notes
Create factual summary including:
- **Always check RELEASE-NOTES.txt file** (note: hyphen, not underscore) for developer-authored release notes under the version number section. These notes start with `[*]`, `[**]`, or `[***]` (stars indicate importance) and **must be included** in the raw release notes
- Only user-facing changes (exclude CI, refactoring, technical debt)
- Prioritize: New features → Improvements → Performance enhancements
- Use positive language (avoid "bug fix", prefer "improved", "enhanced", "resolved")
- Include rough character counts to gauge condensation needed
- Mark changes as WordPress-specific, Jetpack-specific, or both

#### 5. User Confirmation
Present raw notes to user for:
- Accuracy verification
- WordPress vs Jetpack feature classification
- Any missing or incorrect changes
- Approval to proceed with editorialization

#### 6. Editorialization
Transform raw notes using established playful style:
- Keep under 350 characters (accounting for translation expansion)
- Use engaging, user-friendly language
- Reference previous release note styles from step 1
- Create separate versions for WordPress and Jetpack apps
- Focus on user benefits and experience improvements

#### 7. Update Release Notes Files
Once user confirms the editorialized release notes, **replace** the contents of the following files (discard any existing content):
- **WordPress release notes**: `WordPress/metadata/release_notes.txt`
- **Jetpack release notes**: `WordPress/jetpack_metadata/release_notes.txt`

Document any process refinements discovered during execution.

### Content Guidelines
- **Include**: New features, UI improvements, performance enhancements, user experience changes
- **Exclude**: CI changes, code refactoring, dependency updates, internal technical changes
- **Language**: Positive sentiment, avoid "fix" terminology, focus on improvements and enhancements
- **Priority Order**: New features → Improvements → Performance → Other user-facing changes

## PR Description Format

When writing PR descriptions, follow this format:

```markdown
## Description

[Brief description of what this PR changes and why]

## Testing instructions

Test case title:

1. Step 1
2. Step 2
- [ ] Verify expected outcome
3. Step 3
- [ ] Verify another expected outcome

Test case title 2:

1. Step 1
2. Step 2
- [ ] Expected outcome
```

### Format Rules
- **Always output PR descriptions as raw markdown inside a code block** so users can copy them directly
- Use numbered lists for action steps
- Use checkboxes (`- [ ]`) for verifications and expected outcomes
- Organize by test case titles when there are multiple scenarios
