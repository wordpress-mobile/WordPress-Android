package org.wordpress.android.ui.reader.utils

import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils.HtmlScannerListener
import java.util.regex.Pattern

class ReaderEmbedScanner(private val content: String) {
    private val knownEmbeds = HashMap<Pattern, String>()

    init {
        knownEmbeds[Pattern.compile(
            "<blockquote[^<>]class=\"instagram-",
            Pattern.CASE_INSENSITIVE
        )] = "https://platform.instagram.com/en_US/embeds.js"
        knownEmbeds[Pattern.compile(
            "<fb:post",
            Pattern.CASE_INSENSITIVE
        )] = "https://connect.facebook.net/en_US/sdk.js#xfbml=1&amp;version=v2.8"
    }

    fun beginScan(listener: HtmlScannerListener) {
        knownEmbeds.keys
            .filter { it.matcher(content).find() }
            .forEach {
                // Use the onTagFound callback to pass a URL. Not super clean, but avoid clutter with more kind
                // of listeners.
                knownEmbeds[it]?.let { url ->
                    listener.onTagFound("", url)
                }
            }
    }
}
