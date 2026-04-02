package org.wordpress.android.util

import androidx.core.text.HtmlCompat
import javax.inject.Inject

class HtmlCompatWrapper @Inject constructor() {
    fun fromHtml(source: String, flags: Int = HtmlCompat.FROM_HTML_MODE_COMPACT): CharSequence =
        HtmlCompat.fromHtml(source, flags)
}
