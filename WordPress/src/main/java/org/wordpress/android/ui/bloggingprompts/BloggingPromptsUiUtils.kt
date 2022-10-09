package org.wordpress.android.ui.bloggingprompts

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.util.extensions.getString

internal object BloggingPromptsUiUtils {
    fun ImageView.showImage(@DrawableRes resId: Int) {
        visible()
        setImageResource(resId)
    }

    fun TextView.showText(@StringRes resId: Int) {
        visible()
        text = getString(resId)
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.gone() {
        this.visibility = View.GONE
    }
}
