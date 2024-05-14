package org.wordpress.android.ui.debug.preferences

import kotlin.reflect.KClass

/**
 * Class used to track debuggable shared preferences and will show up in [DebugSharedPreferenceFlagsActivity].
 */
enum class DebugPrefs(val key: String, val type: KClass<*>) {
    ALWAYS_SHOW_ANNOUNCEMENT("prefs_always_show_announcement", Boolean::class)
}
