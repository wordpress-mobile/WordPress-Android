package org.wordpress.android.util

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

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
