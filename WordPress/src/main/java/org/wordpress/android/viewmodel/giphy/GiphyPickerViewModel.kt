package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList

/**
 * Holds the data for [GiphyPickerActivity]
 *
 * This creates a [PagedList] which can be bound to by a [PagedListAdapter].
 */
class GiphyPickerViewModel(
    /**
     * The [GiphyPickerDataSourceFactory] to use
     *
     * This is only available in the constructor to allow for mocking with testing. The default value is generally
     * what we want.
     */
    private val dataSourceFactory: GiphyPickerDataSourceFactory = GiphyPickerDataSourceFactory()
) : ViewModel() {

    private val _selectedMediaViewModelList = MutableLiveData<LinkedHashMap<String, GiphyMediaViewModel>>()
    /**
     * A [Map] of the [GiphyMediaViewModel]s that were selected by the user
     *
     * This map is sorted in the order that the user picked them. The [String] is the value of [GiphyMediaViewModel.id].
     */
    val selectedMediaViewModelList: LiveData<LinkedHashMap<String, GiphyMediaViewModel>> = _selectedMediaViewModelList

    val mediaViewModelPagedList: LiveData<PagedList<GiphyMediaViewModel>> by lazy {
        val pagedListConfig = PagedList.Config.Builder().setEnablePlaceholders(true).setPageSize(30).build()
        LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()
    }

    fun search(searchQuery: String) = dataSourceFactory.setSearchQuery(searchQuery)
    /**
     * Toggles a [GiphyMediaViewModel]'s `isSelected` property between true and false
     */
    fun toggleSelected(mediaViewModel: GiphyMediaViewModel) {
        assert(mediaViewModel is MutableGiphyMediaViewModel)
        mediaViewModel as MutableGiphyMediaViewModel

        val isSelected = !(mediaViewModel.isSelected.value ?: false)

        mediaViewModel.postIsSelected(isSelected)

        val selectedList = (selectedMediaViewModelList.value ?: LinkedHashMap()).apply {
            // Add or remove the [mediaViewModel] from the list
            if (isSelected) {
                set(mediaViewModel.id, mediaViewModel)
            } else {
                mediaViewModel.postSelectionNumber(null)
                remove(mediaViewModel.id)
            }
        }

        _selectedMediaViewModelList.postValue(selectedList)
    }
}
