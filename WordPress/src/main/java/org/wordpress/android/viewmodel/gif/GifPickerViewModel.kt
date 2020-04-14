package org.wordpress.android.viewmodel.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedList.BoundaryCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.getDistinct
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

/**
 * Holds the data for [org.wordpress.android.ui.gif.GifPickerActivity]
 *
 * This creates a [PagedList] which can be bound to by a [PagedListAdapter] and also manages the logic of the
 * selected media. That includes but not limited to keeping the [GifMediaViewModel.selectionNumber] continuous.
 *
 * Calling [setup] is required before using this ViewModel.
 */
class GifPickerViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val mediaFetcher: GifMediaFetcher,
    /**
     * The [GifPickerDataSourceFactory] to use
     *
     * This is only available in the constructor to allow mocking in tests.
     */
    private val dataSourceFactory: GifPickerDataSourceFactory
) : CoroutineScopedViewModel() {
    /**
     * A result of [downloadSelected] observed using the [downloadResult] LiveData
     */
    data class DownloadResult(val mediaModels: List<MediaModel>? = null, val errorMessageStringResId: Int? = null)

    enum class State {
        /**
         * The default state where interaction with the UI like selecting and searching is allowed
         */
        IDLE,
        /**
         * This is reached when the user chose some items and pressed the "Add" button
         *
         * We're actively downloading and saving in the background during this state.
         */
        DOWNLOADING,
        /**
         * Reached after [DOWNLOADING] was successful
         *
         * No UI interaction should be allowed during this state and the Activity should already be dismissed.
         */
        FINISHED
    }

    /**
     * Describes how an empty view UI should be displayed
     */
    enum class EmptyDisplayMode {
        HIDDEN,
        /**
         * Visible because the user has not performed a search or the search string is blank.
         */
        VISIBLE_NO_SEARCH_QUERY,
        /**
         * Visible because the user has performed a search but there are no search results
         */
        VISIBLE_NO_SEARCH_RESULTS,
        /**
         * Visible because there was a network error on the first page load.
         */
        VISIBLE_NETWORK_ERROR
    }

    private val _emptyDisplayMode = MutableLiveData<EmptyDisplayMode>().apply {
        value = EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY
    }
    /**
     * Describes how the empty view UI should be displayed
     */
    val emptyDisplayMode: LiveData<EmptyDisplayMode> = _emptyDisplayMode

    /**
     * Errors that happened during page loads.
     *
     * @see [GifPickerDataSource.rangeLoadErrorEvent]
     */
    val rangeLoadErrorEvent: LiveData<Throwable> = dataSourceFactory.rangeLoadErrorEvent

    private lateinit var site: SiteModel

    private val _state = MutableLiveData<State>().apply { value = State.IDLE }
    /**
     * Describes what state this ViewModel (and the corresponding Activity) is in.
     */
    val state: LiveData<State> = _state

    private val _downloadResult = SingleLiveEvent<DownloadResult>()
    /**
     * Produces results whenever [downloadSelected] finishes
     */
    val downloadResult: LiveData<DownloadResult> = _downloadResult

    private val _selectedMediaViewModelList = MutableLiveData<LinkedHashMap<String, GifMediaViewModel>>()
    /**
     * A [Map] of the [GifMediaViewModel]s that were selected by the user
     *
     * This map is sorted in the order that the user picked them. The [String] is the value of [GifMediaViewModel.id].
     */
    val selectedMediaViewModelList: LiveData<LinkedHashMap<String, GifMediaViewModel>> = _selectedMediaViewModelList

    /**
     * Returns `true` if the selection bar (UI) should be shown
     *
     * This changes when the number of items change from 0 to 1 or 1 to 0.
     */
    val selectionBarIsVisible: LiveData<Boolean> =
            Transformations.map(selectedMediaViewModelList) { it.isNotEmpty() }.getDistinct()

    private val _isPerformingInitialLoad = MutableLiveData<Boolean>()
    /**
     * Returns `true` if we are (or going to) perform an initial load due to a [search] call.
     *
     * This will be `false` when the initial load has been executed and completed.
     */
    val isPerformingInitialLoad: LiveData<Boolean> = _isPerformingInitialLoad

    /**
     * The [PagedList] that should be displayed in the RecyclerView
     */
    val mediaViewModelPagedList: LiveData<PagedList<GifMediaViewModel>> by lazy {
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setInitialLoadSizeHint(DEFAULT_INITIAL_LOAD_SIZE_HINT)
                .setPageSize(DEFAULT_PAGE_SIZE)
                .build()

        LivePagedListBuilder(dataSourceFactory, pagedListConfig)
                .setBoundaryCallback(pagedListBoundaryCallback)
                .build()
    }

    /**
     * Update the [emptyDisplayMode] depending on the number of API search results or whether there was an error.
     */
    private val pagedListBoundaryCallback = object : BoundaryCallback<GifMediaViewModel>() {
        override fun onZeroItemsLoaded() {
            _isPerformingInitialLoad.postValue(false)

            val displayMode = when {
                dataSourceFactory.initialLoadError != null -> EmptyDisplayMode.VISIBLE_NETWORK_ERROR
                dataSourceFactory.searchQuery.isBlank() -> EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY
                else -> EmptyDisplayMode.VISIBLE_NO_SEARCH_RESULTS
            }
            _emptyDisplayMode.postValue(displayMode)
            super.onZeroItemsLoaded()
        }

        override fun onItemAtFrontLoaded(itemAtFront: GifMediaViewModel) {
            _isPerformingInitialLoad.postValue(false)
            _emptyDisplayMode.postValue(EmptyDisplayMode.HIDDEN)
            super.onItemAtFrontLoaded(itemAtFront)
        }
    }

    /**
     * Receives the query strings submitted in [search]
     *
     * This is the [ReceiveChannel] used to [debounce] on.
     */
    private val searchQueryChannel = Channel<String>()

    init {
        // Register a [searchQueryChannel] callback which gets called after a set number of time has elapsed.
        launch {
            searchQueryChannel.debounce().consumeEach { search(query = it, immediately = true) }
        }
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
     * Set the current search query.
     *
     * The search will not be executed until a short amount of time has elapsed. This enables us to keep receiving
     * queries from a text field without unnecessarily launching API requests. API requests will only be executed
     * when, presumably, the user has stopped typing.
     *
     * This also clears the [selectedMediaViewModelList]. This makes sense because the user will not be seeing the
     * currently selected [GifMediaViewModel] if the new search query results are different.
     *
     * Searching is disabled if downloading or the [query] is the same as the last one.
     *
     * @param immediately If `true`, bypasses the timeout and immediately executes API requests
     */
    fun search(query: String, immediately: Boolean = false) = launch {
        if (immediately) {
            if (_state.value != State.IDLE) {
                return@launch
            }
            // Do not search if the same. This prevents searching to be re-executed after configuration changes.
            if (dataSourceFactory.searchQuery == query) {
                return@launch
            }

            _isPerformingInitialLoad.postValue(true)

            _selectedMediaViewModelList.postValue(LinkedHashMap())

            // The empty view should be hidden while the user is searching
            _emptyDisplayMode.postValue(EmptyDisplayMode.HIDDEN)

            dataSourceFactory.searchQuery = query

            analyticsTrackerWrapper.track(AnalyticsTracker.Stat.GIF_PICKER_SEARCHED)
        } else {
            searchQueryChannel.send(query)
        }
    }

    /**
     * Downloads all the selected [GifMediaViewModel]
     *
     * When the process is finished, the results will be posted to [downloadResult].
     *
     * If [downloadSelected] is successful, the [DownloadResult.mediaModels] will be a non-null list. If not, the
     * [DownloadResult.errorMessageStringResId] will be non-null and should be shown to the user.
     *
     * This also changes the [state] to:
     *
     * - [State.DOWNLOADING] while downloading
     * - [State.FINISHED] if the download was successful
     * - [State.IDLE] if the download failed
     */
    fun downloadSelected() = launch {
        if (_state.value != State.IDLE) {
            return@launch
        }

        if (!networkUtils.isNetworkAvailable()) {
            // Network is not available to download the selected media, post the result and return
            _downloadResult.postValue(
                    DownloadResult(errorMessageStringResId = R.string.no_network_message)
            )
            return@launch
        }

        _state.postValue(State.DOWNLOADING)

        val result = try {
            val gifMediaViewModels = _selectedMediaViewModelList.value?.values?.toList() ?: emptyList()
            val mediaModels = mediaFetcher.fetchAndSave(gifMediaViewModels, site)
            DownloadResult(mediaModels = mediaModels)
        } catch (e: CancellationException) {
            // We don't need to handle coroutine cancellations. The UI should just do nothing.
            DownloadResult()
        } catch (e: Exception) {
            // No need to log the error because that is already logged by `fetchAndSave()`
            DownloadResult(errorMessageStringResId = R.string.error_downloading_image)
        }

        _downloadResult.postValue(result)
        _state.postValue(if (result.mediaModels != null) State.FINISHED else State.IDLE)
    }

    /**
     * Toggles a [GifMediaViewModel]'s `isSelected` property between true and false
     *
     * This also updates the [GifMediaViewModel.selectionNumber] of all the objects in [selectedMediaViewModelList].
     */
    fun toggleSelected(mediaViewModel: GifMediaViewModel) {
        if (_state.value != State.IDLE) {
            return
        }

        assert(mediaViewModel is MutableGifMediaViewModel)
        mediaViewModel as MutableGifMediaViewModel

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
     * Update the [GifMediaViewModel.selectionNumber] values so that they are continuous
     *
     * For example, if the selection numbers are [1, 2, 3, 4, 5] and the 2nd [GifMediaViewModel] was removed, we
     * want the selection numbers to be updated to [1, 2, 3, 4] instead of leaving it as [1, 3, 4, 5].
     */
    private fun rebuildSelectionNumbers(mediaList: LinkedHashMap<String, GifMediaViewModel>) {
        mediaList.values.forEachIndexed { index, mediaViewModel ->
            (mediaViewModel as MutableGifMediaViewModel).postSelectionNumber(index + 1)
        }
    }

    /**
     * Creates a new [ReceiveChannel] which only produces values after the [timeout] has elapsed and no new values
     * have been received from self ([ReceiveChannel]).
     *
     * This works like Rx's [Debounce operator](http://reactivex.io/documentation/operators/debounce.html).
     */
    private fun <T> ReceiveChannel<T>.debounce(timeout: Long = 300): ReceiveChannel<T> = produce {
        var job: Job? = null

        consumeEach {
            job?.cancel()

            job = launch {
                delay(timeout)
                send(it)
            }
        }
    }

    /**
     * Retries all previously failed page loads.
     *
     * @see [GifPickerDataSource.retryAllFailedRangeLoads]
     */
    fun retryAllFailedRangeLoads() = dataSourceFactory.retryAllFailedRangeLoads()

    companion object {
        private const val DEFAULT_INITIAL_LOAD_SIZE_HINT = 42
        private const val DEFAULT_PAGE_SIZE = 21
    }
}
