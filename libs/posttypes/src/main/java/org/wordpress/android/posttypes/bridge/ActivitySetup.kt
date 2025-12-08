package org.wordpress.android.posttypes.bridge

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

/**
 * Marker interface for activities in the Post Types module.
 *
 * Implement this interface to gain access to [applyBaseSetup] and other
 * module-specific activity extensions.
 *
 * ## Purpose
 * Uses composition over inheritance to avoid extending a base activity class.
 * This makes the module more portable and easier to migrate.
 *
 * ## Migration Notes
 * When merging back into the main app:
 * - Option A: Remove interface, have activities extend `BaseAppCompatActivity`
 * - Option B: Keep the pattern if preferred over inheritance
 *
 * @see applyBaseSetup
 */
interface CptActivity

/**
 * Applies base activity setup for Post Types module activities.
 *
 * Call this in `onCreate()` before `super.onCreate()`:
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     applyBaseSetup()
 *     super.onCreate(savedInstanceState)
 *     // ...
 * }
 * ```
 *
 * ## What it does
 * - Enables edge-to-edge display (handles status bar properly)
 *
 * ## Migration Notes
 * When merging back into the main app:
 * - Replace with `BaseAppCompatActivity` inheritance, or
 * - Inline the setup if preferred
 */
fun <T> T.applyBaseSetup() where T : AppCompatActivity, T : CptActivity {
    enableEdgeToEdge()
}
