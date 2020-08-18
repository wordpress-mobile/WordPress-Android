package org.wordpress.android.ui.accounts

import android.content.res.Resources
import org.wordpress.android.R

object LoginFlowThemeHelper {
    /**
     * This function should be used by activities that use the LoginFlow theme.
     * These activities often use components that refer to custom theme attributes defined by the WordPress theme,
     * but that are missing from the LoginFlow theme. Some examples: wpColorError, wpColorSuccess, etc.
     * Instead of extending the LoginFlow theme only to include these attributes and having to maintain them in multiple
     * places, we use this function to "inject" them directly.
     */
    @JvmStatic fun injectMissingCustomAttributes(theme: Resources.Theme) {
        theme.applyStyle(R.style.WordPress_NoActionBar, false)
    }
}
