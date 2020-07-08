package org.wordpress.android.util

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
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
            ContextCompat.getColor(this, it.resourceId)
        }

@DrawableRes
fun Context.getDrawableResIdFromAttribute(@AttrRes attribute: Int) =
        TypedValue().let {
            theme.resolveAttribute(attribute, it, true)
            it.resourceId
        }

fun Context.getColorStateListFromAttribute(@AttrRes attribute: Int): ColorStateList =
        getColorResIdFromAttribute(attribute).let {
            AppCompatResources.getColorStateList(this, it)
        }

// https://developer.android.com/reference/android/content/res/Configuration.html#locale
val Context.currentLocale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0]
