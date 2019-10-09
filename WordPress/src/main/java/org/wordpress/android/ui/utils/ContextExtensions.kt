package org.wordpress.android.ui.utils

import android.content.Context
import android.os.Build
import java.util.Locale

// https://developer.android.com/reference/android/content/res/Configuration.html#locale
val Context.currentLocale: Locale
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            return resources.configuration.locale
        }
    }
