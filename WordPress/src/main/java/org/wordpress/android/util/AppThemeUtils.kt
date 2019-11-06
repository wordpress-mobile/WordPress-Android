package org.wordpress.android.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat
import org.wordpress.android.R

class AppThemeUtils {
    companion object {
        @SuppressLint("WrongConstant") // we use MODE_NIGHT_AUTO_BATTERY for API <= 27
        @JvmStatic
        fun changeTheme(context: Context, newTheme: String) {
            when (newTheme) {
                context.getString(R.string.app_theme_entry_value_light) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                context.getString(R.string.app_theme_entry_value_dark) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                context.getString(R.string.app_theme_entry_value_default) -> {
                    if (BuildCompat.isAtLeastQ()) {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )
                    } else {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                        )
                    }
                }
                else -> AppLog.w(AppLog.T.UTILS, "Theme key $newTheme is not recognized.")
            }
        }
    }
}
