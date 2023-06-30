package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.WPSwitch

/**
 * SwitchCompat with custom track and thumb drawables and width to match closely the [WPSwitch] composable. This lets
 * us incrementally adopt Compose and use [WPSwitch] even inside Android View-based layouts without big visual
 * differences.
 */
class WPSwitchCompat(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.switchStyle,
) : SwitchCompat(context, attrs, defStyleAttr) {
    init {
        setEnforceSwitchWidth(false)
        trackDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_track)
        thumbDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_thumb)
        switchMinWidth = resources.getDimensionPixelSize(R.dimen.switch_min_width)
    }
}
