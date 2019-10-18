package org.wordpress.android.util

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.os.ConfigurationCompat
import java.util.Locale

@ColorRes
fun Context.getColorResIdFromAttribute(@AttrRes attribute: Int) =
        TypedValue().let {
            theme.resolveAttribute(attribute, it, true)
            it.resourceId
        }

@ColorInt
fun Context.getColorFromAttribute(@AttrRes attribute: Int) =
        TypedValue().let {
            theme.resolveAttribute(attribute, it, true)
            it.data
        }

// https://developer.android.com/reference/android/content/res/Configuration.html#locale
val Context.currentLocale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0]
