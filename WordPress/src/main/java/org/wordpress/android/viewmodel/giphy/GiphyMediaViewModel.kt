package org.wordpress.android.viewmodel.giphy

import android.net.Uri
import com.giphy.sdk.core.models.Media

/**
 * A data-representation of [GiphyMediaViewHolder]
 *
 * The values of this class comes from Giphy's [Media] model. The [Media] object is big so we use this class to
 * only keep a minimal amount of memory. This also hides the complexity of navigating the valus of [Media].
 *
 * See the [Giphy API docs](https://developers.giphy.com/docs/) for more information on what a [Media] object contains.
 * Search for "The GIF Object" section.
 */
class GiphyMediaViewModel(
    /**
     * The id from Giphy's [Media]
     */
    val id: String,
    /**
     * The thumbnail to show in a list. This is an animated GIF.
     */
    val thumbnailUri: Uri,
    /**
     * The title that appears on giphy.com
     */
    val title: String
) {

    constructor(media: Media) : this(
            id = media.id,
            thumbnailUri = Uri.parse(media.images.fixedHeightDownsampled.gifUrl),
            title = media.title
    )
}
