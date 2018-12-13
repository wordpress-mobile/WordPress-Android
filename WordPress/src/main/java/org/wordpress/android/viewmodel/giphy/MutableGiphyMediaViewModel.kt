package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.giphy.sdk.core.models.Media
import org.wordpress.android.viewmodel.SingleLiveEvent

/**
 * A mutable implementation of [GiphyMediaViewModel]
 *
 * This is meant to be accessible by [GiphyPickerViewModel] and [GiphyPickerDataSource] only. This is designed this
 * way so that [GiphyPickerViewModel] encapsulates all the logic of managing selected items as well as keeping their
 * selection numbers continuous.
 *
 * The [GiphyPickerViewHolder] should never have access to the mutating methods of this class.
 */
class MutableGiphyMediaViewModel(
    override val id: String,
    override val thumbnailUri: Uri,
    override val previewImageUri: Uri,
    override val largeImageUri: Uri,
    override val title: String
) : GiphyMediaViewModel {
    /**
     * Using [SingleLiveEvent] will prevent calls like this from running immediately when a ViewHolder is bound:
     *
     * ```
     * mediaViewModel?.isSelected?.observe(this, Observer {
     *      // animate because isSelected changed
     * })
     *
     * ```
     *
     * This is a conscious bleed of UI logic in the business logic. The alternative may lead to more code. In the
     * future, we can extend [LiveData] to have something like a `skip(1)` function. Like this in Rx:
     * http://rxmarbles.com/#skip
     */
    private val _isSelected = SingleLiveEvent<Boolean>()
    override val isSelected: LiveData<Boolean> = _isSelected

    private val _selectionNumber = MutableLiveData<Int?>()
    override val selectionNumber: LiveData<Int?> = _selectionNumber

    constructor(media: Media) : this(
            id = media.id,
            thumbnailUri = Uri.parse(media.images.fixedHeightDownsampled.gifUrl),
            previewImageUri = Uri.parse(media.images.downsized.gifUrl),
            largeImageUri = Uri.parse(media.images.downsizedLarge.gifUrl),
            title = media.title
    )

    fun postIsSelected(isSelected: Boolean) = _isSelected.postValue(isSelected)
    fun postSelectionNumber(number: Int?) = _selectionNumber.postValue(number)
}
