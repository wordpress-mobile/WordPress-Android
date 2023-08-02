package org.wordpress.android.util

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import org.wordpress.android.R

object MissingSplitsUtils {
    /**
     * R.drawable.brush_stroke is the first drawable the app attempts to access. If a NotFoundException occurs while
     * trying to access it, it indicates that there are missing splits, possibly due to sideloading the app.
     */
    fun isMissingSplits(context: Context) = try {
        context.resources.getValue(R.drawable.brush_stroke, TypedValue(), true)
        false
    } catch (e: Resources.NotFoundException) {
        true
    }
}
