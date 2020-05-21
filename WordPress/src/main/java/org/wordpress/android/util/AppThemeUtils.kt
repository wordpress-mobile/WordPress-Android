package org.wordpress.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.wordpress.android.R

class AppThemeUtils {
    companion object {
        @SuppressLint("WrongConstant") // lint suggests deprecated constant for some reason
        @JvmStatic
        @JvmOverloads
        fun setAppTheme(context: Context, newTheme: String? = null) {
            val themeName = if (TextUtils.isEmpty(newTheme)) {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                sharedPreferences
                        .getString(
                                context.getString(R.string.pref_key_app_theme),
                                context.getString(R.string.app_theme_entry_value_default)
                        )
            } else {
                newTheme
            }

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
