package org.wordpress.android.viewmodel.gif

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.viewmodel.SingleLiveEvent

/**
 * A mutable implementation of [GifMediaViewModel]
 *
 * This is meant to be accessible by [GifPickerViewModel] and [GifPickerDataSource] only. This is designed this
 * way so that [GifPickerViewModel] encapsulates all the logic of managing selected items as well as keeping their
 * selection numbers continuous.
 *
 * The [GifPickerViewHolder] should never have access to the mutating methods of this class.
 */
data class MutableGifMediaViewModel(
    override val id: String,
    override val thumbnailUri: Uri,
    override val previewImageUri: Uri,
    override val largeImageUri: Uri,
    override val title: String
) : GifMediaViewModel {
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

    fun postIsSelected(isSelected: Boolean) = _isSelected.postValue(isSelected)
    fun postSelectionNumber(number: Int?) = _selectionNumber.postValue(number)
}
