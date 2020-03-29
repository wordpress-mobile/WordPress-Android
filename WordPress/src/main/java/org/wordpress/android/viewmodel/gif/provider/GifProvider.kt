package org.wordpress.android.viewmodel.gif.provider

import org.wordpress.android.viewmodel.gif.GifMediaViewModel

/**
 * Interface to interact with a GIF provider API avoiding coupling with the concrete implementation
 */
interface GifProvider {
    /**
     * Request GIF search from a query string
     *
     * @param query represents the desired text to search within the provider.
     * @param position to request results starting from a given position for that query
     * @param loadSize to request a result list limited to a specific amount
     * @param onSuccess will be called if the Provider had success finding GIFs
     * and will deliver a [List] of [GifMediaViewModel] with all matching GIFs
     * and a [Int] describing the next position for pagination
     * @param onFailure will be called if the Provider didn't succeed with the task of bringing GIFs,
     * the delivered String will describe the error to be presented to the user
     */
    fun search(
        query: String,
        position: Int,
        loadSize: Int? = null,
        onSuccess: (List<GifMediaViewModel>, Int?) -> Unit,
        onFailure: (Throwable) -> Unit
    )

    /**
     * An Exception to describe errors within the Provider when a [onFailure] is called
     */
    class GifRequestFailedException(message: String) : Exception(message)
}
