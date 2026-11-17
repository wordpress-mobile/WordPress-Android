package org.wordpress.android.ui.reader

import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import javax.inject.Singleton

private const val CSS_ASSET_NAME = "reader-mobile.css"

/**
 * Supplies the base stylesheet for the Reader post detail WebView.
 *
 * The stylesheet used to be pulled from https://wordpress.com/calypso/reader-mobile.css via a
 * `<link>` tag. That made it a render-blocking network request: when the request was slow or
 * stalled mid-response, the article body never painted and the spinner never stopped (CMM-2254).
 * It is now vendored into the app's assets and inlined, so the body always paints from local data.
 */
@Singleton
class ReaderCssProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    @Volatile
    private var cachedCss: String? = null

    /**
     * Returns the contents of the bundled stylesheet, read once and kept in memory. Returns an
     * empty string if the asset can't be read - the renderer's own inline rules are enough to
     * render a readable article, so a missing asset degrades the styling rather than the content.
     */
    @Suppress("TooGenericExceptionCaught")
    fun getBaseCss(): String {
        cachedCss?.let { return it }

        return synchronized(this) {
            cachedCss ?: try {
                contextProvider.getContext().assets.open(CSS_ASSET_NAME)
                    .bufferedReader()
                    .use { it.readText() }
            } catch (e: Exception) {
                AppLog.e(T.READER, "reader css > failed to read bundled stylesheet", e)
                ""
            }.also { cachedCss = it }
        }
    }
}
