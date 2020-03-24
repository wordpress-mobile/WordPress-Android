package org.wordpress.android.viewmodel.gifs.provider

import org.wordpress.android.viewmodel.gifs.GifMediaViewModel

/**
 * Interface to interact with a GIF provider API avoiding coupling with the concrete implementation
 */
interface GifProvider {
    /**
     * Request GIF search from a query string
     *
     * The [query] parameter represents the desired text to search within the provider.
     * [position] to request results starting from a given position for that [query]
     * [loadSize] to request a result list limited to a specific amount
     * [onSuccess] will be called if the Provider had success finding GIFs
     * and will deliver a List<Gif> with all matching GIFs
     * [onFailure] will be called if the Provider didn't succeed with the task of bringing GIFs,
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
     * An Exception to describe errors within the Provider when a [onFailure] is called
     */
    class GifRequestFailedException(message: String) : Exception(message)
}
