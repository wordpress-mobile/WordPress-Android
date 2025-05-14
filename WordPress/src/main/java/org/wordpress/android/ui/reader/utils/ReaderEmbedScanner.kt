package org.wordpress.android.ui.reader.utils

import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils.HtmlScannerListener
import java.util.regex.Pattern

class ReaderEmbedScanner(private val mContent: String) {
    private val mKnownEmbeds = HashMap<Pattern, String>()

    init {
        mKnownEmbeds[Pattern.compile(
            "<blockquote[^<>]class=\"instagram-",
            Pattern.CASE_INSENSITIVE
        )] = "https://platform.instagram.com/en_US/embeds.js"
        mKnownEmbeds[Pattern.compile(
            "<fb:post",
            Pattern.CASE_INSENSITIVE
        )] = "https://connect.facebook.net/en_US/sdk.js#xfbml=1&amp;version=v2.8"
    }

    fun beginScan(listener: HtmlScannerListener) {
        requireNotNull(listener) { "HtmlScannerListener is required" }

        for (pattern in mKnownEmbeds.keys) {
            if (pattern.matcher(mContent).find()) {
                // Use the onTagFound callback to pass a URL. Not super clean, but avoid clutter with more kind
                // of listeners.
                listener.onTagFound("", mKnownEmbeds[pattern])
            }
        }
    }
}
