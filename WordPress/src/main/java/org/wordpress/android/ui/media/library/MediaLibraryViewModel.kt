package org.wordpress.android.ui.media.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.media.VideoLoader
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.PlansConstants
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.viewmodel.ResourceProvider
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Drives the media library: the site's media from the FluxC [MediaStore], filtered by type or a local search,
 * paged in from the server, plus selection, delete and upload retry. Picking, capturing and permissions need an
 * Activity, so the screen handles those and hands the results to [uploader] through this view model.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
    private val siteStore: SiteStore,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val uploadServiceFacade: UploadServiceFacade,
    private val deleteController: MediaDeleteServiceController,
    private val uploader: MediaLibraryUploader,
    private val videoLoader: VideoLoader,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), MediaLibraryActions {
    var site: SiteModel = requireNotNull(savedStateHandle.get<SiteModel>(WordPress.SITE)) {
        "MediaLibraryViewModel requires a site"
    }
        private set

    private val _uiState = MutableStateFlow(
        MediaLibraryUiState(
            filter = savedStateHandle.get<String>(KEY_FILTER)?.let { MediaFilter.valueOf(it) } ?: MediaFilter.ALL,
            selectedIds = savedStateHandle.get<IntArray>(KEY_SELECTED_IDS)?.toList().orEmpty(),
            isSearchActive = savedStateHandle[KEY_SEARCH_ACTIVE] ?: false,
            searchQuery = savedStateHandle[KEY_SEARCH_QUERY] ?: ""
        )
    )
    val uiState: StateFlow<MediaLibraryUiState> = _uiState.asStateFlow()

    // no replay: toasts raised while the screen is stopped (e.g. behind media details) are dropped, not shown later
    private val _events = MutableSharedFlow<MediaLibraryEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<MediaLibraryEvent> = _events.asSharedFlow()

    /** Path the camera writes a captured photo to, kept across process death while the camera app is open */
    var mediaCapturePath: String?
        get() = savedStateHandle[KEY_CAPTURE_PATH]
        set(value) {
            savedStateHandle[KEY_CAPTURE_PATH] = value
        }

    // filters fetched at least once, and filters whose every page has been fetched
    private val fetchedFilters = mutableSetOf<MediaFilter>()
    private val fullyFetchedFilters = mutableSetOf<MediaFilter>()

    // filters with a fetch in flight; each fetch's result carries its mime type, so filters fetch independently
    // the value is whether that fetch is a load-more
    private val fetchingFilters = mutableMapOf<MediaFilter, Boolean>()

    // filters whose pull-to-refresh arrived during a load-more, so their first page is fetched once it lands
    private val pendingRefreshFilters = mutableSetOf<MediaFilter>()

    private var reloadJob: Job? = null
    private var isReloadPending = false
    private var hasAudio = false
    private var isAudioCheckPending = true

    private val videoFrameAllowed = mutableMapOf<String, Boolean>()

    init {
        dispatcher.register(this)
        _uiState.update {
            it.copy(
                canUpload = WPMediaUtils.currentUserCanUploadMedia(site),
                addMediaOptions = buildAddMediaOptions(),
                thumbnailSource = buildThumbnailSource(),
                quota = buildQuota()
            )
        }
        viewModelScope.launch {
            _uiState.collect {
                savedStateHandle[KEY_FILTER] = it.filter.name
                savedStateHandle[KEY_SELECTED_IDS] = it.selectedIds.toIntArray()
                savedStateHandle[KEY_SEARCH_ACTIVE] = it.isSearchActive
                savedStateHandle[KEY_SEARCH_QUERY] = it.searchQuery
            }
        }
        onFilterSelected(_uiState.value.filter)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        deleteController.release()
    }

    override fun onFilterSelected(filter: MediaFilter) {
        _uiState.update {
            it.copy(
                filter = filter,
                // spinners belong to the previous filter's fetch
                isRefreshing = false,
                isLoadingMore = false,
                // the previous filter's error or offline state doesn't apply to this one
                emptyState = when {
                    filter in fetchedFilters -> MediaEmptyState.NO_CONTENT
                    // its first fetch starts once the store reload below finishes
                    networkUtilsWrapper.isNetworkAvailable() -> MediaEmptyState.LOADING
                    else -> MediaEmptyState.NETWORK_ERROR
                }
            )
        }
        val job = reload()
        viewModelScope.launch {
            // wait for the store's items, so the spinner only shows when there's nothing cached to look at
            job.join()
            if (_uiState.value.filter == filter) {
                fetchIfNeverFetched()
            }
        }
    }

    override fun onRefresh() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            showToast(R.string.no_network_message)
            _uiState.update { it.copy(isRefreshing = false, emptyState = MediaEmptyState.NETWORK_ERROR) }
            return
        }
        fetch(loadMore = false, showRefreshIndicator = true)
    }

    override fun onLoadMore() {
        if (_uiState.value.filter !in fullyFetchedFilters) {
            fetch(loadMore = true, showRefreshIndicator = false)
        }
    }

    override fun onSearchOpen() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    override fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, emptyState = MediaEmptyState.NO_CONTENT) }
        reload()
    }

    override fun onSearchClose() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "", emptyState = MediaEmptyState.NO_CONTENT) }
        reload()
    }

    override fun onItemClick(localId: Int) {
        if (_uiState.value.isSelectionActive) {
            toggleSelection(localId)
            return
        }
        val media = mediaStore.getMediaWithLocalId(localId)
        when {
            media == null -> {
                AppLog.w(AppLog.T.MEDIA, "Media library > unable to load localMediaId = $localId")
                showToast(R.string.error_media_load)
            }
            // nothing to show for a failed upload, the tile offers retry and delete instead
            MediaUploadState.fromString(media.uploadState) == MediaUploadState.FAILED -> Unit
            else -> {
                val ids = _uiState.value.items.map { it.localId.toString() }
                _events.tryEmit(MediaLibraryEvent.OpenMediaSettings(media, ArrayList(ids)))
            }
        }
    }

    override fun onItemLongClick(localId: Int) {
        toggleSelection(localId)
    }

    override fun onClearSelection() {
        setSelection(emptyList())
    }

    override fun onDeleteSelectedClick() {
        _uiState.update { it.copy(showDeleteConfirmation = it.selectedIds.isNotEmpty()) }
    }

    override fun onConfirmDelete() {
        val ids = _uiState.value.selectedIds
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        deleteMedia(ids)
        setSelection(emptyList())
    }

    override fun onDismissDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    /** The trash button on a failed or queued upload, which deletes without asking, like the old grid did */
    override fun onItemTrashClick(localId: Int) {
        deleteMedia(listOf(localId))
    }

    override fun onItemRetryClick(localId: Int) {
        val media = mediaStore.getMediaWithLocalId(localId)
        if (media == null) {
            showToast(R.string.error_media_not_found)
        } else {
            uploader.addToUploadService(listOf(media))
        }
    }

    fun onFilesPicked(uris: List<Uri>) {
        viewModelScope.launch { uploader.uploadPickedFiles(site, uris) }
    }

    fun onSharedMedia(uris: List<Uri>) {
        viewModelScope.launch { uploader.uploadSharedMedia(site, uris) }
    }

    fun onPhotoCaptured() {
        val path = mediaCapturePath ?: return
        mediaCapturePath = null
        viewModelScope.launch { uploader.uploadCapturedPhoto(site, path) }
    }

    fun onVideoCaptured(uri: Uri) {
        viewModelScope.launch { uploader.uploadCapturedVideo(site, uri) }
    }

    /** Media was deleted from the details screen, or added by the stock media picker */
    fun onMediaChangedElsewhere() {
        isAudioCheckPending = true
        reload()
        dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
    }

    /** The upload service finished with these items, successfully or not */
    fun onUploadFinished() = reload()

    /** Picks up deletes left pending in the store, e.g. ones interrupted by losing the connection */
    fun resumePendingDeletes() = deleteController.start(site, null)

    fun onConnectionAvailable() {
        if (mediaStore.hasSiteMediaToDelete(site)) {
            deleteController.start(site, null)
        }
    }

    /**
     * Whether a remote video is small enough to pull a frame from for its tile. Coil has to download the video to do
     * that, so big ones keep the placeholder. Answers are cached, since tiles ask again every time they scroll back
     * in; a failed check isn't, so the tile tries again once the connection is back.
     */
    override suspend fun canLoadVideoFrame(url: String): Boolean =
        videoFrameAllowed[url]
            ?: videoLoader.checkMediaNotTooBig(url)?.also { videoFrameAllowed[url] = it }
            ?: false

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        if (event.site.id != site.id) return

        val eventFilter = MediaFilter.fromMimeType(event.mimeType)
        val wasLoadMore = fetchingFilters.remove(eventFilter) == true
        if (eventFilter == _uiState.value.filter) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false) }
        }

        if (event.isError) {
            onFetchError(event, eventFilter)
        } else {
            onFetchSuccess(event, eventFilter, isFirstPage = !wasLoadMore)
        }

        if (pendingRefreshFilters.remove(eventFilter) && eventFilter == _uiState.value.filter) {
            fetch(loadMore = false, showRefreshIndicator = true)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: OnMediaChanged) {
        if (event.mediaList.isNotEmpty() && event.mediaList.none { it.localSiteId == site.id }) return
        if (event.isError) {
            AppLog.w(AppLog.T.MEDIA, "Received onMediaChanged error: ${event.error.type} - ${event.error.message}")
            val message = resourceProvider.getString(R.string.media_generic_error)
            showToast(event.error.message?.takeIf { it.isNotEmpty() }?.let { "$message. $it" } ?: message)
            return
        }
        if (event.cause == MediaAction.DELETE_MEDIA) {
            val deletedIds = event.mediaList.map { it.id }.toSet()
            setSelection(_uiState.value.selectedIds.filterNot { it in deletedIds })
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
        }
        reload()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        val media = event.media
        if (media != null && media.localSiteId != site.id) return

        val isFinished = event.completed || event.isError || event.canceled
        if (event.completed || event.isError) {
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
        }
        // progress ticks arrive up to ten times a second, so only reload when a visible tile would change - FluxC
        // marks an upload UPLOADING without an OnMediaChanged, so the first tick is what flips the tile
        val current = media?.let { m -> _uiState.value.items.firstOrNull { it.localId == m.id } }
        val tileChanged = current != null && current.uploadState != MediaUploadState.fromString(media.uploadState)
        if (media == null || isFinished || tileChanged) {
            reload()
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        val updated = siteStore.getSiteByLocalId(site.id) ?: return
        site = updated
        _uiState.update { it.copy(quota = buildQuota(), thumbnailSource = buildThumbnailSource()) }
    }

    private fun onFetchSuccess(event: OnMediaListFetched, eventFilter: MediaFilter, isFirstPage: Boolean) {
        if (isFirstPage) {
            // FluxC drops the cached uploaded media of this filter's type that isn't on the first page, which
            // leaves the overlapping filters (every other one for ALL, ALL for a type) with a partial list, so
            // they start over from their first page
            val overlapping = if (eventFilter == MediaFilter.ALL) MediaFilter.entries else listOf(MediaFilter.ALL)
            (overlapping - eventFilter).forEach {
                fetchedFilters -= it
                fullyFetchedFilters -= it
            }
        }
        fetchedFilters += eventFilter
        if (event.canLoadMore) {
            fullyFetchedFilters -= eventFilter
        } else if (eventFilter == MediaFilter.ALL) {
            // every item of every type has been fetched, so the other filters have nothing more to fetch
            fetchedFilters += MediaFilter.entries
            fullyFetchedFilters += MediaFilter.entries
        } else {
            fullyFetchedFilters += eventFilter
        }
        if (eventFilter == _uiState.value.filter) {
            _uiState.update { it.copy(emptyState = MediaEmptyState.NO_CONTENT) }
        }
        isAudioCheckPending = true
        reload()
    }

    private fun onFetchError(event: OnMediaListFetched, eventFilter: MediaFilter) {
        AppLog.e(AppLog.T.MEDIA, "Media error occurred: ${event.error.type}")
        // stop paging that filter until the next refresh, so a failing request isn't retried on every scroll; ALL
        // covers every type, so its failure stops them all
        if (eventFilter == MediaFilter.ALL) {
            fullyFetchedFilters += MediaFilter.entries
        } else {
            fullyFetchedFilters += eventFilter
        }
        if (eventFilter != _uiState.value.filter) {
            updateCanLoadMore()
            return
        }
        val isPermissionError = event.error.type == MediaErrorType.AUTHORIZATION_REQUIRED
        val emptyState = if (isPermissionError) MediaEmptyState.PERMISSION_ERROR else MediaEmptyState.GENERIC_ERROR
        // the empty view shows the same message, so only toast when there's content covering it
        if (_uiState.value.items.isNotEmpty()) {
            showToast(if (isPermissionError) R.string.media_error_no_permission else R.string.error_refresh_media)
        }
        _uiState.update { it.copy(emptyState = emptyState) }
        updateCanLoadMore()
    }

    private fun fetchIfNeverFetched() {
        if (_uiState.value.filter !in fetchedFilters && networkUtilsWrapper.isNetworkAvailable()) {
            fetch(loadMore = false, showRefreshIndicator = _uiState.value.items.isEmpty())
        }
    }

    private fun fetch(loadMore: Boolean, showRefreshIndicator: Boolean) {
        val state = _uiState.value
        when {
            !networkUtilsWrapper.isNetworkAvailable() ->
                _uiState.update { it.copy(isRefreshing = false, emptyState = MediaEmptyState.NETWORK_ERROR) }
            // search runs against the local store only
            state.searchQuery.isNotEmpty() -> _uiState.update { it.copy(isRefreshing = false) }
            // already fetching this filter, so just keep the spinner up if one was asked for
            state.filter in fetchingFilters -> {
                if (!loadMore && fetchingFilters[state.filter] == true) {
                    pendingRefreshFilters += state.filter
                }
                if (showRefreshIndicator) {
                    _uiState.update { it.copy(isRefreshing = true) }
                }
            }
            else -> startFetch(state.filter, loadMore, showRefreshIndicator)
        }
    }

    private fun startFetch(filter: MediaFilter, loadMore: Boolean, showRefreshIndicator: Boolean) {
        fetchingFilters[filter] = loadMore
        _uiState.update {
            it.copy(
                isRefreshing = showRefreshIndicator,
                isLoadingMore = loadMore,
                emptyState = MediaEmptyState.LOADING
            )
        }
        val payload = FetchMediaListPayload(site, NUM_MEDIA_PER_FETCH, loadMore, filter.mimeType, null)
        dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        if (!loadMore) {
            // refreshes the space quota
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
        }
    }

    /**
     * Reloads the grid from the local store. Store events come in bursts (one per item of a batch delete or
     * upload), so a reload asked for while one runs is coalesced into a single follow-up pass.
     */
    private fun reload(): Job {
        reloadJob?.takeIf { it.isActive }?.let {
            isReloadPending = true
            return it
        }
        return viewModelScope.launch {
            do {
                isReloadPending = false
                val filter = _uiState.value.filter
                val query = _uiState.value.searchQuery
                val checkAudio = isAudioCheckPending
                isAudioCheckPending = false
                val idsBeingDeleted = deleteController.idsBeingDeleted()
                val (items, audioFound) = withContext(bgDispatcher) {
                    ensureCorrectState(queryMedia(filter, query), idsBeingDeleted).map { it.toGridItem() } to
                            if (checkAudio) mediaStore.getSiteAudio(site).isNotEmpty() else null
                }
                audioFound?.let { hasAudio = it }
                _uiState.update {
                    it.copy(
                        items = items,
                        itemsFilter = filter,
                        itemsQuery = query,
                        // keep the audio tab while it's selected, so the selection doesn't vanish from the user
                        filters = if (hasAudio || it.filter == MediaFilter.AUDIO) {
                            MediaFilter.entries
                        } else {
                            MediaLibraryUiState.DEFAULT_FILTERS
                        }
                    )
                }
            } while (isReloadPending)
            updateCanLoadMore()
        }.also { reloadJob = it }
    }

    private fun updateCanLoadMore() {
        _uiState.update {
            it.copy(canLoadMore = it.filter !in fullyFetchedFilters && it.searchQuery.isEmpty())
        }
    }

    private fun queryMedia(filter: MediaFilter, query: String): List<MediaModel> =
        if (query.isNotEmpty()) {
            when (filter) {
                MediaFilter.IMAGES -> mediaStore.searchSiteImages(site, query)
                MediaFilter.DOCUMENTS -> mediaStore.searchSiteDocuments(site, query)
                MediaFilter.VIDEOS -> mediaStore.searchSiteVideos(site, query)
                MediaFilter.AUDIO -> mediaStore.searchSiteAudio(site, query)
                MediaFilter.ALL -> mediaStore.searchSiteMedia(site, query)
            }
        } else {
            when (filter) {
                MediaFilter.IMAGES -> mediaStore.getSiteImages(site)
                MediaFilter.DOCUMENTS -> mediaStore.getSiteDocuments(site)
                MediaFilter.VIDEOS -> mediaStore.getSiteVideos(site)
                MediaFilter.AUDIO -> mediaStore.getSiteAudio(site)
                MediaFilter.ALL -> mediaStore.getAllSiteMedia(site)
            }
        }

    /**
     * (1) Re-applies DELETING to media still being deleted, since a refresh may have overwritten it, and (2) drops
     * local media whose file no longer exists, e.g. because the user deleted it from the device.
     */
    private fun ensureCorrectState(media: List<MediaModel>, idsBeingDeleted: Set<Int>): List<MediaModel> =
        media.filter { item ->
            if (item.id in idsBeingDeleted) {
                item.setUploadState(MediaUploadState.DELETING)
            }
            val filePath = item.filePath
            val isMissingLocalFile =
                filePath != null && MediaUtils.isLocalFile(item.uploadState) && !File(filePath).exists()
            if (isMissingLocalFile) {
                AppLog.w(AppLog.T.MEDIA, "removing nonexistent local media $filePath")
                dispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(item))
            }
            !isMissingLocalFile
        }

    private fun deleteMedia(localIds: List<Int>) {
        val mediaList = localIds.mapNotNull { mediaStore.getMediaWithLocalId(it) }
        // deleting from the server needs a connection - check before anything is marked DELETING, since nothing
        // resets that state and the items would be left stuck showing "Deleting"
        if (mediaList.any { !isLocalMedia(it) } && !networkUtilsWrapper.isNetworkAvailable()) {
            showToast(R.string.no_network_message)
            return
        }

        val mediaToDelete = mutableListOf<MediaModel>()
        mediaList.forEach { media ->
            // if uploading, first issue a cancel upload command
            if (uploadServiceFacade.isPendingOrInProgressMediaUpload(media)) {
                val payload = CancelMediaPayload(site, media, false)
                dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
            }
            if (isLocalMedia(media)) {
                dispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(media))
            } else {
                media.setUploadState(MediaUploadState.DELETING)
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
                mediaToDelete += media
            }
        }

        if (mediaList.size != localIds.size) {
            showToast(R.string.cannot_delete_multi_media_items)
        }
        if (mediaToDelete.isNotEmpty()) {
            deleteController.start(site, mediaToDelete)
        }
    }

    private fun isLocalMedia(media: MediaModel) =
        media.uploadState?.let { MediaUtils.isLocalFile(it.lowercase(Locale.ROOT)) } == true

    private fun toggleSelection(localId: Int) {
        val selected = _uiState.value.selectedIds
        when {
            localId in selected -> setSelection(selected - localId)
            _uiState.value.items.firstOrNull { it.localId == localId }?.canSelect == true ->
                setSelection(selected + localId)
        }
    }

    private fun setSelection(ids: List<Int>) {
        _uiState.update { it.copy(selectedIds = ids) }
    }

    private fun showToast(messageResId: Int) = showToast(resourceProvider.getString(messageResId))

    private fun showToast(message: String) {
        _events.tryEmit(MediaLibraryEvent.ShowToast(message))
    }

    private fun buildAddMediaOptions(): List<AddMediaOption> = buildList {
        add(AddMediaOption.CAPTURE_PHOTO)
        add(AddMediaOption.CAPTURE_VIDEO)
        add(AddMediaOption.CHOOSE_FILE)
        if (site.isUsingWpComRestApi && buildConfigWrapper.isJetpackApp) {
            add(AddMediaOption.STOCK_MEDIA)
        }
    }

    private fun buildThumbnailSource() = MediaThumbnailSource(
        usePhoton = SiteUtils.isPhotonCapable(site),
        isPrivateAtomic = site.isPrivateWPComAtomic
    )

    private fun buildQuota(): MediaQuotaUiState? {
        if (!site.hasDiskSpaceQuotaInformation()) return null
        val units = FILE_SIZE_UNITS.map { resourceProvider.getString(it) }.toTypedArray()
        val quota = if (site.planId == PlansConstants.BUSINESS_PLAN_ID) {
            val space = FormatUtils.formatFileSize(site.spaceUsed, units)
            resourceProvider.getString(R.string.site_settings_quota_space_unlimited, space)
        } else {
            val percentage = FormatUtils.formatPercentageLimit100(site.spacePercentUsed / PERCENT, true)
            val space = FormatUtils.formatFileSize(site.spaceAllowed, units)
            resourceProvider.getString(R.string.site_settings_quota_space_value, percentage, space)
        }
        return MediaQuotaUiState(
            text = resourceProvider.getString(R.string.media_space_used, quota),
            isNearLimit = site.spacePercentUsed > QUOTA_WARNING_PERCENT
        )
    }

    private fun MediaModel.toGridItem(): MediaGridItemUiState {
        val type = when {
            mimeType?.startsWith("image/") == true -> MediaGridItemType.IMAGE
            isVideo -> MediaGridItemType.VIDEO
            else -> MediaGridItemType.OTHER
        }
        val localPath = when (type) {
            MediaGridItemType.IMAGE -> filePath?.takeIf { it.isNotEmpty() && MediaUtils.isLocalFile(uploadState) }
            // the tile only falls back to the local file when there's no usable thumbnail
            MediaGridItemType.VIDEO -> {
                val hasThumbnail = !thumbnailUrl.isNullOrEmpty() && !MediaUtils.isVideo(thumbnailUrl)
                filePath?.takeIf { !hasThumbnail && it.isNotEmpty() && File(it).exists() }
            }
            MediaGridItemType.OTHER -> null
        }
        return MediaGridItemUiState(
            localId = id,
            type = type,
            fileName = fileName.orEmpty(),
            mimeType = mimeType,
            displayName = title?.takeIf { it.isNotEmpty() } ?: fileName.orEmpty(),
            localFilePath = localPath,
            url = url,
            mediumLargeUrl = fileUrlMediumLargeSize,
            mediumUrl = fileUrlMediumSize,
            largeUrl = fileUrlLargeSize,
            thumbnailUrl = thumbnailUrl,
            uploadState = MediaUploadState.fromString(uploadState)
        )
    }

    companion object {
        // a multiple of both column counts (3 in portrait, 4 in landscape), so a page fills whole rows
        private const val NUM_MEDIA_PER_FETCH = 48
        private const val EVENT_BUFFER = 8
        private const val PERCENT = 100.0
        private const val QUOTA_WARNING_PERCENT = 90

        private val FILE_SIZE_UNITS = listOf(
            R.string.file_size_in_bytes,
            R.string.file_size_in_kilobytes,
            R.string.file_size_in_megabytes,
            R.string.file_size_in_gigabytes,
            R.string.file_size_in_terabytes
        )

        private const val KEY_FILTER = "media_library_filter"
        private const val KEY_SELECTED_IDS = "media_library_selected_ids"
        private const val KEY_SEARCH_ACTIVE = "media_library_search_active"
        private const val KEY_SEARCH_QUERY = "media_library_search_query"
        private const val KEY_CAPTURE_PATH = "media_library_capture_path"
    }
}
