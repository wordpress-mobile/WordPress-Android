/**
 * # Bridge Package
 *
 * This package contains temporary bridging code that connects the isolated Post Types module
 * to the main WordPress app module.
 *
 * ## Purpose
 *
 * The Post Types module is intentionally isolated to:
 * 1. **Prevent accidental FluxC usage** - By not depending on FluxC, the compiler will catch
 *    any accidental imports of legacy patterns
 * 2. **Enable clean wordpress-rs integration** - Forces deliberate decisions about data models
 *    and API integration from the start
 * 3. **Establish new architectural patterns** - Acts as a sandbox for the new service layer
 *
 * ## Contents
 *
 * - [BridgeConstants] - Intent extra keys and other constants that mirror main app values
 * - [BridgeTheme] - Standalone Material3 theme (temporary replacement for AppThemeM3)
 * - [SiteReference] - Minimal site representation (temporary replacement for SiteModel)
 *
 * ## Migration Guide
 *
 * When this module is merged back into the main app or fully integrated with wordpress-rs:
 *
 * ### If keeping wordpress-rs integration:
 * 1. Replace [SiteReference] with wordpress-rs site models
 * 2. Replace [BridgeTheme] with `AppThemeM3`
 * 3. Replace [BridgeConstants.EXTRA_SITE] with `WordPress.SITE`
 * 4. Delete this entire bridge package
 *
 * ### If reverting to main module (no wordpress-rs):
 * 1. Move all non-bridge files to `WordPress/src/main/java/org/wordpress/android/ui/posttypes/`
 * 2. Replace bridge imports with main app equivalents:
 *    - `BridgeTheme` → `AppThemeM3`
 *    - `SiteReference` → `SiteModel`
 *    - `BridgeConstants.EXTRA_SITE` → `WordPress.SITE`
 * 3. Delete this module from `settings.gradle`
 * 4. Remove the module dependency from `WordPress/build.gradle`
 *
 * ## Adding New Bridge Items
 *
 * If you need to add new bridging code:
 * 1. Add it to this package
 * 2. Document the equivalent main app type/constant in KDoc
 * 3. Add migration notes explaining how to replace it
 */
package org.wordpress.android.posttypes.bridge
