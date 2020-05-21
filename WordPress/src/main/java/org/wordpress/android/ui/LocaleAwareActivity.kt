package org.wordpress.android.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.util.LocaleManager

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
 */
abstract class LocaleAwareActivity : AppCompatActivity() {
    /**
     * Used to update locales on API 21 to API 25.
     */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    /**
     * Used to update locales on API 26 and beyond.
     */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(LocaleManager.updatedConfigLocale(baseContext, overrideConfiguration))
    }
}
