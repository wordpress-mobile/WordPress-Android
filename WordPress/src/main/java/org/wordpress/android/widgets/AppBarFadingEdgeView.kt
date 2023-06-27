package org.wordpress.android.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
import android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.StateSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.elevation.ElevationOverlayProvider
import org.wordpress.android.R
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import android.R as AndroidR
import com.google.android.material.R as MaterialR

class AppBarFadingEdgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    init {
        iniView()
    }

    // state_lifted and state_liftable are actually not private, and this waring appears only in code (xml is ok)
    // is gone on newer versions of gradle
    @SuppressLint("PrivateResource")
    private fun iniView() {
        isDuplicateParentStateEnabled = true

        val elevationOverlayProvider = ElevationOverlayProvider(context)
        val appbarElevation = resources.getDimension(R.dimen.appbar_elevation)
        val appBarColor = elevationOverlayProvider.compositeOverlayIfNeeded(
            context.getColorFromAttribute(R.attr.wpColorAppBar),
            appbarElevation
        )

        val fadingEdgeDrawableElevated = GradientDrawable(
            if (RtlUtils.isRtl(context)) {
                LEFT_RIGHT
            } else {
                RIGHT_LEFT
            },
            intArrayOf(ContextCompat.getColor(context, AndroidR.color.transparent), appBarColor)
        )

        val fadingEdgeDrawableFlush = GradientDrawable(
            if (RtlUtils.isRtl(context)) {
                LEFT_RIGHT
            } else {
                RIGHT_LEFT
            },
            intArrayOf(
                ContextCompat.getColor(context, AndroidR.color.transparent),
                context.getColorFromAttribute(R.attr.wpColorAppBar)
            )
        )

        val fadingAgeStateListDrawable = StateListDrawable()
        fadingAgeStateListDrawable.addState(
            intArrayOf(
                -MaterialR.attr.state_lifted,
                MaterialR.attr.state_liftable,
                AndroidR.attr.state_enabled
            ), fadingEdgeDrawableFlush
        )
        fadingAgeStateListDrawable.addState(StateSet.WILD_CARD, fadingEdgeDrawableElevated)
        background = fadingAgeStateListDrawable
    }
}
