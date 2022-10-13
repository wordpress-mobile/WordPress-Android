package org.wordpress.android.userflags.resolver

import org.wordpress.android.ui.prefs.AppPrefs.PrefKey

data class UserFlagsPrefKey(private val name: String) : PrefKey {
    override fun name(): String = name
}
