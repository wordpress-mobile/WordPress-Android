package org.wordpress.android.ui.utils

import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class HtmlMessageUtils
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun getHtmlMessageFromStringFormatResId(@StringRes formatResId: Int, vararg args: Any?): CharSequence {
        return HtmlCompat.fromHtml(
            String.format(resourceProvider.getString(formatResId), *args),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    fun getHtmlMessageFromStringFormat(text: String, vararg args: Any?): CharSequence {
        return HtmlCompat.fromHtml(
            String.format(text, *args),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}
