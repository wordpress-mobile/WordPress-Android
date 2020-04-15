package org.wordpress.android.viewmodel.gif

import android.net.Uri
import androidx.lifecycle.LiveData

/**
 * A data-representation of [GifMediaViewHolder]
 *
 * The values of this class comes from [GifProvider] as a list.
 *
 * This class also houses the selection status. The [GifMediaViewHolder] observes the [isSelected] and
 * [selectionNumber] properties to update itself.
 */
interface GifMediaViewModel {
    /**
     * The GIF unique id
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
     * The title that appears on the GIF
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
