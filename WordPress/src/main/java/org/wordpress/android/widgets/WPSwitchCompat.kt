package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.WPSwitch
import com.google.android.material.R as MaterialR

/**
 * SwitchCompat with custom track and thumb drawables and width to match closely the [WPSwitch] composable. This lets
 * us incrementally adopt Compose and use [WPSwitch] even inside Android View-based layouts without big visual
 * differences.
 *
 * Important: since this view was made to match [WPSwitch], in turn matching [androidx.compose.material.Switch], it is
 * going to be necessary to double check the appearance of this whenever the Material Compose lib is updated (since that
 * can lead to visual changes in [androidx.compose.material.Switch]).
 */
class WPSwitchCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = MaterialR.attr.switchStyle,
) : SwitchCompat(context, attrs, defStyleAttr) {
    init {
        setEnforceSwitchWidth(false)
        trackDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_track)
        thumbDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_thumb)
        switchMinWidth = resources.getDimensionPixelSize(R.dimen.switch_min_width)
    }
}
