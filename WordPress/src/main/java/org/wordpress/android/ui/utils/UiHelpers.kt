package org.wordpress.android.ui.utils

import org.wordpress.android.R
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.utils.UiDimen.UIDimenDPInt
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject
import android.graphics.Point
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams

class UiHelpers @Inject constructor() {
    fun getPxOfUiDimen(context: Context, uiDimen: UiDimen): Int =
            when (uiDimen) {
                is UIDimenRes -> context.resources.getDimensionPixelSize(uiDimen.dimenRes)
                is UIDimenDPInt -> DisplayUtils.dpToPx(context, uiDimen.dimensionDP)
            }

    fun getTextOfUiString(context: Context, uiString: UiString): String =
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
        view.text = text ?: ""
    }

    fun setImageOrHide(imageView: ImageView, @DrawableRes resId: Int?) {
        updateVisibility(imageView, resId != null)
        resId?.let {
            imageView.setImageResource(resId)
        }
    }

    companion object {
        fun adjustDialogSize(dialog: Dialog) {
            val window = dialog.window
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
