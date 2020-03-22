package org.wordpress.android.ui.gifs.provider

import org.wordpress.android.R

interface GifProvider {
    fun search(
        query: String,
        onSuccess: (List<Gif>) -> Unit,
        onFailure: (String) -> Unit
    )

    data class Gif(val url: String)

    companion object {
        const val queryReturnedNothingStringId = R.string.gifs_list_search_nothing_found
        const val unknownErrorStringId = R.string.error
    }
}
