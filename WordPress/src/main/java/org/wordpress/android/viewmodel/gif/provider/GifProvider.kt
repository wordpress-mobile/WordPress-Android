package org.wordpress.android.viewmodel.gif.provider

import android.net.Uri
import org.wordpress.android.R
import org.wordpress.android.viewmodel.gif.GifMediaViewModel

/**
 * Interface to interact with a GIF provider API avoiding coupling with the concrete implementation
 */
interface GifProvider {
    /**
     * Request GIF search given a query string
     *
     * The [query] parameter represents the desired text to be search within the provider.
     * [position] to request results starting from a given position for that [query]
     * [loadSize] to request a result list limited to a specific amount
     * [onSuccess] will be called if the Provider had success finding GIFs
     * and will deliver a List<Gif> with all matching GIFs
     * [onFailure] will be called if the Provider didn't succeeded with the task of bringing GIFs,
     * the delivered String will describe the error to be presented to the user
     */
    fun search(
        query: String,
        position: Int,
        loadSize: Int? = null,
        onSuccess: (List<GifMediaViewModel>) -> Unit,
        onFailure: (Throwable) -> Unit
    )

    /**
     * A class to represent the default data model delivered by any [GifProvider] implementation
     */
    data class Gif(
        val thumbnailUri: Uri,
        val previewImageUri: Uri,
        val largeImageUri: Uri
    )

    /**
     * Exception to describe errors within the Provider when a [onFailure] is called
     */
    class GifRequestFailedException(message: String) : Exception(message)
}
