package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.getDistinct
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

/**
 * Holds the data for [org.wordpress.android.ui.giphy.GiphyPickerActivity]
 *
 * This creates a [PagedList] which can be bound to by a [PagedListAdapter] and also manages the logic of the
 * selected media. That includes but not limited to keeping the [GiphyMediaViewModel.selectionNumber] continuous.
 *
 * Calling [setup] is required before using this ViewModel.
 */
class GiphyPickerViewModel @Inject constructor(
    private val mediaFetcher: GiphyMediaFetcher,
    /**
     * The [GiphyPickerDataSourceFactory] to use
     *
     * This is only available in the constructor to allow mocking in tests.
     */
    private val dataSourceFactory: GiphyPickerDataSourceFactory
) : CoroutineScopedViewModel() {
    enum class State {
        IDLE,
        DOWNLOADING,
        DONE
    }

    private lateinit var site: SiteModel

    private val _state = MutableLiveData<State>().apply { value = State.IDLE }
    val state: LiveData<State> = _state

    private val _downloadResult = SingleLiveEvent<Pair<List<MediaModel>?, Int?>>()
    val downloadResult: LiveData<Pair<List<MediaModel>?, Int?>> = _downloadResult

    private val _selectedMediaViewModelList = MutableLiveData<LinkedHashMap<String, GiphyMediaViewModel>>()
    /**
     * A [Map] of the [GiphyMediaViewModel]s that were selected by the user
     *
     * This map is sorted in the order that the user picked them. The [String] is the value of [GiphyMediaViewModel.id].
     */
    val selectedMediaViewModelList: LiveData<LinkedHashMap<String, GiphyMediaViewModel>> = _selectedMediaViewModelList

    /**
     * Returns `true` if the selection bar (UI) should be shown
     *
     * This changes when the number of items change from 0 to 1 or 1 to 0.
     */
    val selectionBarIsVisible: LiveData<Boolean> =
            Transformations.map(selectedMediaViewModelList) { it.isNotEmpty() }.getDistinct()

    val mediaViewModelPagedList: LiveData<PagedList<GiphyMediaViewModel>> by lazy {
        val pagedListConfig = PagedList.Config.Builder().setEnablePlaceholders(true).setPageSize(30).build()
        LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()
    }

    /**
     * Perform additional initialization for this ViewModel
     *
     * The [site] usually comes from this ViewModel's corresponding Activity
     */
    fun setup(site: SiteModel) {
        this.site = site
    }

    /**
     * Set the current search query
     *
     * This also clears the [selectedMediaViewModelList]. This makes sense because the user will not be seeing the
     * currently selected [GiphyMediaViewModel] if the new search query results are different.
     */
    fun search(searchQuery: String) {
        if (_state.value != State.IDLE) {
            return
        }

        _selectedMediaViewModelList.postValue(LinkedHashMap())
        dataSourceFactory.setSearchQuery(searchQuery)
    }

    /**
     * Downloads the selected [GiphyMediaViewModel]
     */
    fun downloadSelected() = launch {
        _state.postValue(State.DOWNLOADING)

        val uris = (_selectedMediaViewModelList.value?.values?.toList() ?: emptyList()).map {
            it.largeImageUri
        }

        val eventValue = try {
            val mediaModels = mediaFetcher.fetchAndSave(uris, site)
            Pair(mediaModels, null)
        } catch (e: CancellationException) {
            // We don't need to handle coroutine cancellations. The UI should just do nothing.
            Pair(null, null)
        } catch (e: Exception) {
            Pair(null, R.string.error_downloading_image)
        }

        _downloadResult.postValue(eventValue)
        _state.postValue(if (eventValue.first != null) State.DONE else State.IDLE)
    }

    /**
     * Toggles a [GiphyMediaViewModel]'s `isSelected` property between true and false
     *
     * This also updates the [GiphyMediaViewModel.selectionNumber] of all the objects in [selectedMediaViewModelList].
     */
    fun toggleSelected(mediaViewModel: GiphyMediaViewModel) {
        if (_state.value != State.IDLE) {
            return
        }

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

            rebuildSelectionNumbers(this)
        }

        _selectedMediaViewModelList.postValue(selectedList)
    }

    /**
     * Update the [GiphyMediaViewModel.selectionNumber] values so that they are continuous
     *
     * For example, if the selection numbers are [1, 2, 3, 4, 5] and the 2nd [GiphyMediaViewModel] was removed, we
     * want the selection numbers to be updated to [1, 2, 3, 4] instead of leaving it as [1, 3, 4, 5].
     */
    private fun rebuildSelectionNumbers(mediaList: LinkedHashMap<String, GiphyMediaViewModel>) {
        mediaList.values.forEachIndexed { index, mediaViewModel ->
            (mediaViewModel as MutableGiphyMediaViewModel).postSelectionNumber(index + 1)
        }
    }
}
