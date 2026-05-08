package org.wordpress.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R

class AppThemeUtils {
    companion object {
        @SuppressLint("WrongConstant") // lint suggests deprecated constant for some reason
        @JvmStatic
        @JvmOverloads
        fun setAppTheme(context: Context, newTheme: String? = null) {
            val themeName = if (TextUtils.isEmpty(newTheme)) {
                readThemeFromPrefs(context)
            } else {
                newTheme
            }
            applyNightMode(context, themeName)
        }

        // Reads SharedPreferences off the main thread before applying the theme. The first read of
        // default SharedPreferences blocks on disk I/O, which has caused ANRs during app startup.
        fun setAppThemeAsync(context: Context, scope: CoroutineScope) {
            scope.launch(Dispatchers.IO) {
                val themeName = readThemeFromPrefs(context)
                withContext(Dispatchers.Main) {
                    applyNightMode(context, themeName)
                }
            }
        }

        private fun readThemeFromPrefs(context: Context): String? =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString(
                    context.getString(R.string.pref_key_app_theme),
                    context.getString(R.string.app_theme_entry_value_default)
                )

        @SuppressLint("WrongConstant") // lint suggests deprecated constant for some reason
        private fun applyNightMode(context: Context, themeName: String?) {
            when (themeName) {
                context.getString(R.string.app_theme_entry_value_light) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                context.getString(R.string.app_theme_entry_value_dark) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                context.getString(R.string.app_theme_entry_value_default) -> {
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    )
                }
                else -> AppLog.w(AppLog.T.UTILS, "Theme key $themeName is not recognized.")
            }
        }
    }
}
