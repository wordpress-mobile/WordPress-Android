package org.wordpress.android.ui.utils

import android.content.Context
import androidx.annotation.ColorRes
import org.wordpress.android.util.HtmlUtils
import javax.inject.Inject

class HtmlUtilsWrapper @Inject constructor() {
    fun colorResToHtmlColor(context: Context, @ColorRes colorRes: Int): String =
        HtmlUtils.colorResToHtmlColor(context, colorRes)
}
