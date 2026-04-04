package org.wordpress.android.ui.reader

import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * Detects whether a tapped image belongs to a gallery in a Reader
 * post WebView, and returns the gallery's image URLs if so.
 */
class ReaderGalleryDetector {
    /**
     * Evaluates JS in [webView] to determine whether the image at
     * [imageUrl] is inside a gallery. Calls [callback] with the
     * gallery's image URLs, or null if the image is not in a gallery.
     */
    fun detectGallery(
        webView: WebView,
        imageUrl: String,
        callback: (ArrayList<String>?) -> Unit
    ) {
        val js = buildDetectionJs(imageUrl)
        webView.evaluateJavascript(js) { result ->
            callback(parseResult(result))
        }
    }

    /**
     * Builds a JavaScript snippet that finds the tapped image in the
     * DOM by matching its URL pathname, walks up to the nearest
     * gallery container, and returns a JSON array of all image URLs
     * in that gallery. The [imageUrl] is escaped to prevent
     * injection before being interpolated into the script.
     */
    private fun buildDetectionJs(imageUrl: String): String {
        val safeUrl = imageUrl
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "")
            .replace("\r", "")
        return """
            (function() {
                try {
                    var tp = new URL('$safeUrl').pathname;
                    var strip = function(p) {
                        return p.replace(/-\d+x\d+(\.[^.]+)${'$'}/, '${'$'}1');
                    };
                    var sel = '.wp-block-gallery,.tiled-gallery,'
                        + '.gallery,.blocks-gallery-grid';
                    var imgs = document.querySelectorAll('img');
                    for (var i = 0; i < imgs.length; i++) {
                        try {
                            var p = new URL(imgs[i].src).pathname;
                            if (p === tp || strip(p) === strip(tp)) {
                                var g = imgs[i].closest(sel);
                                if (!g) return null;
                                var urls = [];
                                var gi = g.querySelectorAll('img');
                                for (var j = 0; j < gi.length; j++) {
                                    if (gi[j].src
                                        && gi[j].src.startsWith('http'))
                                        urls.push(gi[j].src);
                                }
                                return JSON.stringify(urls);
                            }
                        } catch(e) {}
                    }
                    return null;
                } catch(e) { return null; }
            })()
        """.trimIndent()
    }

    /**
     * Parses the JSON string [result] returned by the gallery
     * detection JavaScript. Returns the list of image URLs when
     * the gallery contains more than one image, or null if the
     * result is empty, not valid JSON, or a single-image gallery.
     */
    private fun parseResult(result: String?): ArrayList<String>? {
        if (result.isNullOrEmpty() || result == "null") return null
        return try {
            val json = if (result.startsWith("\"") && result.endsWith("\"")) {
                result
                    .substring(1, result.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            } else {
                result
            }
            val array = JSONArray(json)
            // Single-image galleries fall back to all-images behavior
            // so the viewer shows more context rather than a lone image.
            if (array.length() <= 1) {
                null
            } else {
                val urls = ArrayList<String>(array.length())
                for (i in 0 until array.length()) {
                    urls.add(array.getString(i))
                }
                urls
            }
        } catch (e: JSONException) {
            AppLog.e(T.READER, "Failed to parse gallery URLs: $e")
            null
        }
    }
}
