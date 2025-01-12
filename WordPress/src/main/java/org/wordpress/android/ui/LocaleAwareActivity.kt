package org.wordpress.android.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.PerAppLocaleManager

/**
 * Newer versions of the AppCompat library no longer support locale changes at application level,
 * so this activity is used to help handle those changes at activity level.
 * Reference: https://issuetracker.google.com/issues/141869006#comment9
 *
 * All the actual logic is inside the LocaleManager class, which should be used directly in cases where
 * extending from this class is not possible/preferable.
 *
 * Note: please be mindful of the principle of favoring composition over inheritance and refrain from
 * building upon this class unless it's absolutely necessary.
 *
 * Update Dec 2024: We've added experimental support for per-app language preferences which
 * will eventually negate the need for this class. Instead of extending from this class, we
 * should extend from AppCompatActivity once this feature is out of the experimental phase.
 */
abstract class LocaleAwareActivity : AppCompatActivity() {
    /**
     * Used to update locales on API 21 to API 25.
     */
    override fun attachBaseContext(newBase: Context?) {
        if (isPerAppLocaleEnabled()) {
            super.attachBaseContext(newBase)
        } else {
            super.attachBaseContext(LocaleManager.setLocale(newBase))
        }
    }

    /**
     * Used to update locales on API 26 and beyond.
     */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (isPerAppLocaleEnabled()) {
            super.applyOverrideConfiguration(overrideConfiguration)
        } else {
            super.applyOverrideConfiguration(LocaleManager.updatedConfigLocale(baseContext, overrideConfiguration))
        }
    }

    /**
     * Ideally we would use [PerAppLocaleManager.isPerAppLanguagePrefsEnabled] here, but we
     * can't inject [PerAppLocaleManager] into an abstract class
     */
    private fun isPerAppLocaleEnabled(): Boolean {
        return AppPrefs.getExperimentalFeatureConfig(PerAppLocaleManager.EXPERIMENTAL_PER_APP_LANGUAGE_PREF_KEY)
    }
}
