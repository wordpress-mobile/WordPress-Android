package org.wordpress.android.ui.reader.utils

import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils.HtmlScannerListener
import java.util.regex.Pattern

class ReaderIframeScanner(private val content: String) {
    fun beginScan(listener: HtmlScannerListener) {
        val matcher = IFRAME_TAG_PATTERN.matcher(content)
        while (matcher.find()) {
            val tag = matcher.group(0).orEmpty()
            val src = matcher.group(1).orEmpty()
            listener.onTagFound(tag, src)
        }
    }

    companion object {
        private val IFRAME_TAG_PATTERN: Pattern = Pattern.compile(
            "<iframe[^>]* src=['\"]([^'\"]*)['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
    }
}
