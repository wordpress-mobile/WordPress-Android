package org.wordpress.android.ui.utils

import android.app.Dialog
import android.graphics.Point
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration

/**
 * Sets the [firstView] visible and the [secondView] invisible
 * with a fade in/out animation and vice versa.
 * @param visible if true the [firstView] is shown and the
 *   [secondView] is hidden else the other way round
 */
fun UiHelpers.fadeInfadeOutViews(
    firstView: View?,
    secondView: View?,
    visible: Boolean
) {
    if (firstView == null ||
        secondView == null ||
        visible == firstView.isVisible
    ) return
    if (visible) {
        AniUtils.fadeIn(firstView, Duration.SHORT)
        AniUtils.fadeOut(
            secondView, Duration.SHORT, View.INVISIBLE
        )
    } else {
        AniUtils.fadeIn(secondView, Duration.SHORT)
        AniUtils.fadeOut(
            firstView, Duration.SHORT, View.INVISIBLE
        )
    }
}

@Suppress("DEPRECATION")
fun adjustDialogSize(dialog: Dialog) {
    val window = requireNotNull(dialog.window)
    val size = Point()

    val display = window.windowManager.defaultDisplay
    display.getSize(size)

    val width = size.x

    val maximumWidth = window.context.resources
        .getDimension(R.dimen.alert_dialog_max_width).toInt()
    @Suppress("MagicNumber")
    var proposedWidth = (width * 0.8).toInt()

    if (proposedWidth > maximumWidth) {
        proposedWidth = maximumWidth
    }

    window.setLayout(proposedWidth, LayoutParams.WRAP_CONTENT)
}
