package org.wordpress.android.viewmodel.gif.provider

import org.wordpress.android.R

/**
 * Interface to interact with a GIF provider API avoiding coupling with the concrete implementation
 */
interface GifProvider {
    /**
     * Request GIF search given a query string
     *
     * The [query] parameter represents the desired text to be search within the provider.
     * [onSuccess] will be called if the Provider had success finding GIFs
     * and will deliver a List<Gif> with all matching GIFs
     * [onFailure] will be called if the Provider didn't succeeded with the task of bringing GIFs,
     * the delivered String will describe the error to be presented to the user
     */
    fun search(
        query: String,
        onSuccess: (List<Gif>) -> Unit,
        onFailure: (String) -> Unit
    )

    /**
     * A class to represent the default data model delivered by any [GifProvider] implementation
     */
    data class Gif(val url: String)

    /**
     *  String resources to describe failures when a [onFailure] is called
     */
    companion object {
        const val queryReturnedNothingStringId = R.string.gifs_list_search_nothing_found
        const val unknownErrorStringId = R.string.error
    }
}
