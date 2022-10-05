package org.wordpress.android.ui.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiDimen.UIDimenDPInt
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.StringUtils
import javax.inject.Inject

class UiHelpers @Inject constructor() {
    fun getPxOfUiDimen(context: Context, uiDimen: UiDimen): Int =
            when (uiDimen) {
                is UIDimenRes -> context.resources.getDimensionPixelSize(uiDimen.dimenRes)
                is UIDimenDPInt -> DisplayUtils.dpToPx(context, uiDimen.dimensionDP)
            }

    fun getTextOfUiString(context: Context, uiString: UiString): CharSequence =
            when (uiString) {
                is UiStringRes -> context.getString(uiString.stringRes)
                is UiStringText -> uiString.text
                is UiStringResWithParams -> context.getString(
                        uiString.stringRes,
                        *uiString.params.map { value ->
                            getTextOfUiString(
                                    context,
                                    value
                            )
                        }.toTypedArray()
                )
                // Current localization process does not support <plurals> resource strings,
                // so we need to use multiple string resources. Switch to @PluralRes in UiStringPluralRes and
                // use context.resources.getQuantityString here when <plurals> is supported by localization process.
                is UiStringPluralRes -> StringUtils.getQuantityString(
                        context,
                        uiString.zeroRes,
                        uiString.oneRes,
                        uiString.otherRes,
                        uiString.count
                )
            }

    fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setTextOrHide(view: TextView, uiString: UiString?) {
        val text = uiString?.let { getTextOfUiString(view.context, uiString) }
        setTextOrHide(view, text)
    }

    fun setTextOrHide(view: TextView, @StringRes resId: Int?) {
        val text = resId?.let { view.context.getString(resId) }
        setTextOrHide(view, text)
    }

    fun setTextOrHide(view: TextView, text: CharSequence?) {
        updateVisibility(view, text != null)
        text?.let {
            view.text = text
        }
    }

    fun setImageOrHide(imageView: ImageView, @DrawableRes resId: Int?) {
        updateVisibility(imageView, resId != null)
        resId?.let {
            imageView.setImageResource(resId)
        }
    }

    /**
     * Sets the [firstView] visible and the [secondView] invisible with a fade in/out animation and vice versa
     * @param visible if true the [firstView] is shown and the [secondView] is hidden else the other way round
     */
    fun fadeInfadeOutViews(firstView: View?, secondView: View?, visible: Boolean) {
        if (firstView == null || secondView == null || visible == (firstView.visibility == View.VISIBLE)) return
        if (visible) {
            AniUtils.fadeIn(firstView, Duration.SHORT)
            AniUtils.fadeOut(secondView, Duration.SHORT, View.INVISIBLE)
        } else {
            AniUtils.fadeIn(secondView, Duration.SHORT)
            AniUtils.fadeOut(firstView, Duration.SHORT, View.INVISIBLE)
        }
    }

    companion object {
        fun adjustDialogSize(dialog: Dialog) {
            val window = requireNotNull(dialog.window)
            val size = Point()

            val display = window.windowManager.defaultDisplay
            display.getSize(size)

            val width = size.x

            val maximumWidth = window.context.resources.getDimension(R.dimen.alert_dialog_max_width).toInt()
            var proposedWidth = (width * 0.8).toInt()

            if (proposedWidth > maximumWidth) {
                proposedWidth = maximumWidth
            }

            window.setLayout(proposedWidth, LayoutParams.WRAP_CONTENT)
        }
    }
}
