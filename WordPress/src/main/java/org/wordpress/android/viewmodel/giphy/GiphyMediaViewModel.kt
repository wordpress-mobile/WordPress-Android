package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.net.Uri
import com.giphy.sdk.core.models.Media

/**
 * A data-representation of [GiphyMediaViewHolder]
 *
 * The values of this class comes from Giphy's [Media] model. The [Media] object is big so we use this class to
 * only keep a minimal amount of memory. This also hides the complexity of navigating the values of [Media].
 *
 * This class also houses the selection status. The [GiphyMediaViewHolder] observes the [isSelected] and
 * [selectionNumber] properties to update itself.
 *
 * See the [Giphy API docs](https://developers.giphy.com/docs/) for more information on what a [Media] object contains.
 * Search for "The GIF Object" section.
 */
interface GiphyMediaViewModel {
    /**
     * The id from Giphy's [Media]
     */
    val id: String
    /**
     * The thumbnail to show in a list. This is an animated GIF.
     */
    val thumbnailUri: Uri
    /**
     * The image to use for previews in the picker
     *
     * This should be the `downsized` image which is downsized to be under 2mb.
     */
    val previewImageUri: Uri
    /**
     * The large image to download.
     *
     * This should be the `downsized_large` which is downsized to be under 8mb.
     */
    val largeImageUri: Uri
    /**
     * The title that appears on giphy.com
     */
    val title: String
    /**
     * Denotes whether this object was selected by the user.
     */
    val isSelected: LiveData<Boolean>
    /**
     * The position of this object in the list of "selected" objects.
     *
     * For example, if this object was the second selected item by the user, the value should be "2".
     */
    val selectionNumber: LiveData<Int?>
}
