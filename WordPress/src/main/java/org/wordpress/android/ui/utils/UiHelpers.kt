package org.wordpress.android.ui.utils

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.text.Spannable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class UiHelpers @Inject constructor() {
    fun getTextOfUiString(context: Context, uiString: UiString): String =
            when (uiString) {
                is UiStringRes -> context.getString(uiString.stringRes)
                is UiStringText -> uiString.text
            }

    fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setTextOrHide(view: TextView, uiString: UiString?) {
        view.visibility = if (uiString == null) View.GONE else View.VISIBLE
        uiString?.let {
            view.text = getTextOfUiString(view.context, uiString)
        }
    }

    fun setTextOrHide(view: TextView, @StringRes resId: Int?) {
        val uiString = if (resId != null) {
            UiStringRes(resId)
        } else {
            null
        }
        setTextOrHide(view, uiString)
    }

    fun setTextOrHide(view: TextView, text: String?) {
        val uiString = if (text != null) {
            UiStringText(text)
        } else {
            null
        }
        setTextOrHide(view, uiString)
    }

    fun setTextOrHide(view: TextView, text: Spannable?) {
        view.visibility = if (text == null) View.GONE else View.VISIBLE
        text?.let {
            view.text = text
        }
    }

    fun setImageOrHide(imageView: ImageView, @DrawableRes resId: Int?) {
        imageView.visibility = if (resId == null) View.GONE else View.VISIBLE
        resId?.let {
            imageView.setImageResource(resId)
        }
    }
}
