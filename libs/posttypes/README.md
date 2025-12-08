# Post Types Module

This module contains the experimental Custom Post Types (CPT) feature implementation.

## Purpose

The Post Types module is **intentionally isolated** from the main WordPress app module to:

1. **Prevent accidental FluxC usage** - By not depending on FluxC, the compiler catches any
   accidental imports of legacy patterns. This is a key goal during wordpress-rs integration.

2. **Enable clean wordpress-rs integration** - Forces deliberate decisions about data models and
   API integration from the start, rather than falling back to familiar FluxC patterns.

3. **Establish new architectural patterns** - Acts as a sandbox for the new service layer before
   patterns are propagated to the rest of the codebase.

## Module Structure

```
libs/posttypes/
├── build.gradle
├── README.md
└── src/main/
    ├── AndroidManifest.xml
    ├── java/org/wordpress/android/posttypes/
    │   ├── bridge/                    # ⚠️ Temporary bridging code
    │   │   ├── package-info.kt        # Documentation for bridge package
    │   │   ├── ActivitySetup.kt       # CptActivity interface + applyBaseSetup()
    │   │   ├── BridgeConstants.kt     # Intent keys mirroring main app
    │   │   ├── BridgeTheme.kt         # Standalone Material3 theme
    │   │   └── SiteReference.kt       # Minimal site representation
    │   ├── CptPostTypesActivity.kt
    │   ├── CptPostTypesViewModel.kt
    │   ├── CptFlatPostListActivity.kt
    │   ├── CptFlatPostListViewModel.kt
    │   └── compose/
    │       ├── CptPostTypesScreen.kt
    │       └── CptFlatPostListScreen.kt
    └── res/values/
        ├── strings.xml
        └── styles.xml              # Cpt.NoActionBar theme
```

## The Bridge Package

The `bridge` package contains **temporary coupling code** that connects this isolated module to
the main WordPress app. Everything in this package is explicitly marked as "needs attention when
merging back or integrating wordpress-rs properly."

### Bridge Components

| Component | Purpose | Main App Equivalent |
|-----------|---------|---------------------|
| `CptActivity` + `applyBaseSetup()` | Activity setup (edge-to-edge, etc.) | `BaseAppCompatActivity` |
| `@style/Cpt.NoActionBar` | Activity theme (no action bar) | `@style/WordPress.NoActionBar` |
| `BridgeConstants.EXTRA_SITE` | Intent extra key for site data | `WordPress.SITE` |
| `CptTheme` | Standalone Material3 Compose theme | `AppThemeM3` |
| `SiteReference` | Minimal site data model | `SiteModel` (FluxC) |

### Why Composition Over Inheritance?

Activities implement `CptActivity` interface and call `applyBaseSetup()` instead of extending
a base class. This approach:

- Avoids "is-a" inheritance problems (fragile base class, tight coupling)
- Makes the setup explicit and discoverable
- Easier to migrate - just remove interface and inline or replace the setup
- Can be extended with additional lifecycle hooks if needed

## Migration Guide

### Option A: Full wordpress-rs Integration (Recommended)

When wordpress-rs integration is complete:

1. Replace `SiteReference` with wordpress-rs site models
2. Replace `CptTheme` with `AppThemeM3` (or keep module theme if preferred)
3. Replace `BridgeConstants.EXTRA_SITE` with `WordPress.SITE`
4. For activities: either extend `BaseAppCompatActivity` or keep `CptActivity` pattern
5. Delete the entire `bridge` package
6. Keep module structure for continued isolation

### Option B: Merge Back to Main Module

If reverting to the main module structure:

1. Move all non-bridge files to:
   ```
   WordPress/src/main/java/org/wordpress/android/ui/posttypes/
   ```

2. Replace bridge imports with main app equivalents:
   - `CptActivity` + `applyBaseSetup()` → extend `BaseAppCompatActivity`
   - `@style/Cpt.NoActionBar` → `@style/WordPress.NoActionBar` (in AndroidManifest)
   - `CptTheme` → `AppThemeM3`
   - `SiteReference` → `SiteModel`
   - `BridgeConstants.EXTRA_SITE` → `WordPress.SITE`

3. Update `ActivityLauncher.viewPostTypes()` to pass `SiteModel` directly

4. Remove module from `settings.gradle`:
   ```diff
   - include ':libs:posttypes'
   ```

5. Remove dependency from `WordPress/build.gradle`:
   ```diff
   - implementation project(":libs:posttypes")
   ```

6. Add activities back to `WordPress/src/main/AndroidManifest.xml`

7. Delete `libs/posttypes/` directory

## Dependencies

This module intentionally has minimal dependencies:

- **AndroidX AppCompat** - Base activity class
- **Jetpack Compose** - UI framework
- **Hilt** - Dependency injection
- **Kotlin Parcelize** - For `SiteReference` parcelable

**Not included (by design):**
- FluxC - Networking/data layer
- WordPress utils - App-specific utilities
- Any other main app dependencies

## Adding New Features

When adding new functionality to this module:

1. **Do not add FluxC dependencies** - If you need site/post data, integrate with wordpress-rs
   or add to the bridge package with clear documentation

2. **Document any new bridge items** - If you must add bridging code, add it to the `bridge`
   package with migration notes in both the code and this README

3. **Keep the module self-contained** - The goal is isolation; dependencies should flow inward,
   not outward

## Testing

```bash
# Build the module
./gradlew :libs:posttypes:assembleDebug

# Build the full app (verifies integration)
./gradlew assembleWordPressVanillaDebug
```
