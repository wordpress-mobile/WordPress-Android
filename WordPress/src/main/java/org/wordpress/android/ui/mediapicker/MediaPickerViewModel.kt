package org.wordpress.android.ui.mediapicker

import android.Manifest.permission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.EditMedia
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.Exit
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.IconClickEvent
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.PreviewMedia
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.PreviewUrl
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.ChooserContext
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenCameraForPhotos
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenCameraForWPStories
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenSystemPicker
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.SwitchMediaPicker
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.CapturePhoto
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.ChooseFromAndroidDevice
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.SwitchSource
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.WpStoriesCapture
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.ENABLED
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.HIDDEN
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.STORIES
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel.Hidden
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel.Visible
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandlerFactory
import org.wordpress.android.ui.mediapicker.loader.MediaLoader
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.DomainModel
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.NextPage
import org.wordpress.android.ui.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PermissionsHandler
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.distinct
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class MediaPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val mediaLoaderFactory: MediaLoaderFactory,
    private val mediaInsertHandlerFactory: MediaInsertHandlerFactory,
    private val mediaPickerTracker: MediaPickerTracker,
    private val permissionsHandler: PermissionsHandler,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val mediaStore: MediaStore,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(mainDispatcher) {
    private lateinit var mediaLoader: MediaLoader
    private lateinit var mediaInsertHandler: MediaInsertHandler
    private val loadActions = Channel<LoadAction>()
    private var searchJob: Job? = null
    private val _domainModel = MutableLiveData<DomainModel>()
    private val _selectedIds = MutableLiveData<List<Identifier>?>()
    private val _onPermissionsRequested = MutableLiveData<Event<PermissionsRequested>>()
    private val _softAskRequest = MutableLiveData<SoftAskRequest>()
    private val _searchExpanded = MutableLiveData<Boolean>()
    private val _showProgressDialog = MutableLiveData<ProgressDialogUiModel>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onNavigate = MutableLiveData<Event<MediaNavigationEvent>>()

    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage
    val onNavigate = _onNavigate as LiveData<Event<MediaNavigationEvent>>

    val onPermissionsRequested: LiveData<Event<PermissionsRequested>> = _onPermissionsRequested

    val uiState: LiveData<MediaPickerUiState> = merge(
            _domainModel.distinct(),
            _selectedIds.distinct(),
            _softAskRequest,
            _searchExpanded,
            _showProgressDialog.distinct()
    ) { domainModel, selectedIds, softAskRequest, searchExpanded, progressDialogUiModel ->
        MediaPickerUiState(
                buildUiModel(domainModel, selectedIds, softAskRequest, searchExpanded),
                buildSoftAskView(softAskRequest),
                FabUiModel(mediaPickerSetup.cameraSetup != HIDDEN && selectedIds.isNullOrEmpty(), this::clickOnCamera),
                buildActionModeUiModel(selectedIds, domainModel?.domainItems),
                buildSearchUiModel(softAskRequest?.let { !it.show } ?: true, domainModel?.filter, searchExpanded),
                !domainModel?.domainItems.isNullOrEmpty() && domainModel?.isLoading == true,
                buildBrowseMenuUiModel(softAskRequest, searchExpanded),
                progressDialogUiModel ?: Hidden
        )
    }

    private fun buildSearchUiModel(isVisible: Boolean, filter: String?, searchExpanded: Boolean?): SearchUiModel {
        return when {
            searchExpanded == true -> SearchUiModel.Expanded(filter ?: "", !mediaPickerSetup.defaultSearchView)
            isVisible -> SearchUiModel.Collapsed
            else -> SearchUiModel.Hidden
        }
    }

    private fun buildBrowseMenuUiModel(softAskRequest: SoftAskRequest?, searchExpanded: Boolean?): BrowseMenuUiModel {
        val isSoftAskRequestVisible = softAskRequest?.show ?: false
        val isSearchExpanded = searchExpanded ?: false
        val showActions = !isSoftAskRequestVisible && !isSearchExpanded
        val showSystemPicker = mediaPickerSetup.systemPickerEnabled && showActions

        return if (showActions && (showSystemPicker || mediaPickerSetup.availableDataSources.isNotEmpty())) {
            val actions = mutableSetOf<BrowseAction>()
            if (showSystemPicker) {
                actions.add(BrowseAction.SYSTEM_PICKER)
            }
            actions.addAll(mediaPickerSetup.availableDataSources.map {
                when (it) {
                    DEVICE -> BrowseAction.DEVICE
                    WP_LIBRARY -> BrowseAction.WP_MEDIA_LIBRARY
                    STOCK_LIBRARY -> BrowseAction.STOCK_LIBRARY
                    GIF_LIBRARY -> BrowseAction.GIF_LIBRARY
                }
            })
            BrowseMenuUiModel(actions)
        } else {
            BrowseMenuUiModel(setOf())
        }
    }

    var lastTappedIcon: MediaPickerIcon? = null
    private lateinit var mediaPickerSetup: MediaPickerSetup
    private var site: SiteModel? = null

    private fun buildUiModel(
        domainModel: DomainModel?,
        selectedIds: List<Identifier>?,
        softAskRequest: SoftAskRequest?,
        isSearching: Boolean?
    ): PhotoListUiModel {
        val data = domainModel?.domainItems
        return if (null != softAskRequest && softAskRequest.show) {
            PhotoListUiModel.Hidden
        } else if (data != null && data.isNotEmpty()) {
            populateDomainItems(data, selectedIds, domainModel)
        } else if (domainModel?.emptyState != null) {
            PhotoListUiModel.Empty(
                    domainModel.emptyState.title,
                    domainModel.emptyState.htmlSubtitle,
                    domainModel.emptyState.image,
                    domainModel.emptyState.bottomImage,
                    domainModel.emptyState.bottomImageDescription,
                    isSearching == true,
                    retryAction = if (domainModel.emptyState.isError) {
                        this::retry
                    } else {
                        null
                    }
            )
        } else if (domainModel?.isLoading == true) {
            PhotoListUiModel.Loading
        } else {
            val onlyMediaType = if (domainModel?.mediaTypes?.size == 1) domainModel.mediaTypes.first() else null
            val stringId = when (onlyMediaType) {
                IMAGE -> R.string.media_empty_image_list
                AUDIO -> R.string.media_empty_audio_list
                VIDEO -> R.string.media_empty_videos_list
                DOCUMENT -> R.string.media_empty_documents_list
                else -> R.string.media_empty_list
            }
            PhotoListUiModel.Empty(
                    UiStringRes(stringId),
                    image = R.drawable.img_illustration_media_105dp,
                    isSearching = isSearching == true
            )
        }
    }

    private fun populateDomainItems(
        data: List<MediaItem>,
        selectedIds: List<Identifier>?,
        domainModel: DomainModel
    ): PhotoListUiModel.Data {
        val uiItems = data.map {
            val showOrderCounter = mediaPickerSetup.canMultiselect
            val toggleAction = MediaPickerUiItem.ToggleAction(it.identifier, showOrderCounter, this::toggleItem)
            val clickAction = MediaPickerUiItem.ClickAction(it.identifier, it.type == VIDEO, this::clickItem)
            val (selectedOrder, isSelected) = if (selectedIds != null && selectedIds.contains(it.identifier)) {
                val selectedOrder = if (showOrderCounter) selectedIds.indexOf(it.identifier) + 1 else null
                val isSelected = true
                selectedOrder to isSelected
            } else {
                null to false
            }

            val fileExtension = it.mimeType?.let { mimeType ->
                mediaUtilsWrapper.getExtensionForMimeType(mimeType).uppercase(localeManagerWrapper.getLocale())
            }
            when (it.type) {
                IMAGE -> MediaPickerUiItem.PhotoItem(
                        url = it.url,
                        identifier = it.identifier,
                        isSelected = isSelected,
                        selectedOrder = selectedOrder,
                        showOrderCounter = showOrderCounter,
                        toggleAction = toggleAction,
                        clickAction = clickAction
                )
                VIDEO -> MediaPickerUiItem.VideoItem(
                        url = it.url,
                        identifier = it.identifier,
                        isSelected = isSelected,
                        selectedOrder = selectedOrder,
                        showOrderCounter = showOrderCounter,
                        toggleAction = toggleAction,
                        clickAction = clickAction
                )
                AUDIO, DOCUMENT -> MediaPickerUiItem.FileItem(
                        fileName = it.name ?: "",
                        fileExtension = fileExtension,
                        identifier = it.identifier,
                        isSelected = isSelected,
                        selectedOrder = selectedOrder,
                        showOrderCounter = showOrderCounter,
                        toggleAction = toggleAction,
                        clickAction = clickAction
                )
            }
        }
        return if (domainModel.hasMore) {
            loadNextPage(uiItems, domainModel)
        } else {
            PhotoListUiModel.Data(items = uiItems)
        }
    }

    private fun loadNextPage(
        uiItems: List<MediaPickerUiItem>,
        domainModel: DomainModel
    ): PhotoListUiModel.Data {
        val updatedItems = uiItems.toMutableList()
        val loaderItem = if (domainModel.emptyState?.isError == true) {
            MediaPickerUiItem.NextPageLoader(false) {
                launch {
                    retry()
                }
            }
        } else {
            MediaPickerUiItem.NextPageLoader(true) {
                launch {
                    loadActions.send(NextPage)
                }
            }
        }
        updatedItems.add(loaderItem)
        return PhotoListUiModel.Data(items = updatedItems)
    }

    private fun buildActionModeUiModel(
        selectedIds: List<Identifier>?,
        items: List<MediaItem>?
    ): ActionModeUiModel {
        val numSelected = selectedIds?.size ?: 0
        if (selectedIds.isNullOrEmpty()) {
            return ActionModeUiModel.Hidden
        }
        val title: UiString? = when {
            numSelected == 0 -> null
            mediaPickerSetup.canMultiselect -> {
                UiStringText(String.format(resourceProvider.getString(R.string.cab_selected), numSelected))
            }
            else -> {
                val isImagePicker = mediaPickerSetup.allowedTypes.contains(IMAGE)
                val isVideoPicker = mediaPickerSetup.allowedTypes.contains(VIDEO)
                val isAudioPicker = mediaPickerSetup.allowedTypes.contains(AUDIO)
                if (isImagePicker && isVideoPicker) {
                    UiStringRes(R.string.photo_picker_use_media)
                } else if (isVideoPicker) {
                    UiStringRes(R.string.photo_picker_use_video)
                } else if (isAudioPicker) {
                    UiStringRes(R.string.photo_picker_use_audio)
                } else {
                    UiStringRes(R.string.photo_picker_use_photo)
                }
            }
        }

        val onlyImagesSelected = items?.none { it.type != IMAGE && selectedIds.contains(it.identifier) } ?: true
        val showEditActionButton = mediaPickerSetup.editingEnabled && onlyImagesSelected
        return ActionModeUiModel.Visible(
                title,
                EditActionUiModel(
                        isVisible = showEditActionButton,
                        isCounterBadgeVisible = if (!showEditActionButton) {
                            false
                        } else {
                            mediaPickerSetup.canMultiselect
                        },
                        counterBadgeValue = numSelected
                )
        )
    }

    fun refreshData(forceReload: Boolean) {
        if (!permissionsHandler.hasStoragePermission()) {
            return
        }
        launch(bgDispatcher) {
            loadActions.send(LoadAction.Refresh(forceReload))
        }
    }

    private fun retry() {
        launch(bgDispatcher) {
            loadActions.send(LoadAction.Retry)
        }
    }

    fun clearSelection() {
        if (!_selectedIds.value.isNullOrEmpty()) {
            mediaPickerTracker.trackSelectionCleared(mediaPickerSetup)
            _selectedIds.postValue(listOf())
        }
    }

    fun start(
        selectedIds: List<Identifier>?,
        mediaPickerSetup: MediaPickerSetup,
        lastTappedIcon: MediaPickerIcon?,
        site: SiteModel?
    ) {
        _selectedIds.value = selectedIds
        this.mediaPickerSetup = mediaPickerSetup
        this.lastTappedIcon = lastTappedIcon
        this.site = site
        if (_domainModel.value == null) {
            mediaPickerTracker.trackMediaPickerOpened(mediaPickerSetup)
            this.mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)
            this.mediaInsertHandler = mediaInsertHandlerFactory.build(mediaPickerSetup, site)
            launch {
                mediaLoader.loadMedia(loadActions).flowOn(bgDispatcher).collect { domainModel ->
                    _domainModel.value = domainModel
                }
            }
            if (!mediaPickerSetup.requiresStoragePermissions || permissionsHandler.hasStoragePermission()) {
                launch(bgDispatcher) {
                    loadActions.send(LoadAction.Start())
                }
            }
        }
        if (mediaPickerSetup.defaultSearchView) {
            _searchExpanded.postValue(true)
        }
    }

    fun numSelected(): Int {
        return _selectedIds.value?.size ?: 0
    }

    fun selectedIdentifiers(): List<Identifier> {
        return _selectedIds.value ?: listOf()
    }

    private fun toggleItem(identifier: Identifier, canMultiselect: Boolean) {
        val updatedUris = _selectedIds.value?.toMutableList() ?: mutableListOf()
        if (updatedUris.contains(identifier)) {
            mediaPickerTracker.trackItemUnselected(mediaPickerSetup)
            updatedUris.remove(identifier)
        } else {
            mediaPickerTracker.trackItemSelected(mediaPickerSetup)
            if (updatedUris.isNotEmpty() && !canMultiselect) {
                updatedUris.clear()
            }
            updatedUris.add(identifier)
        }
        _selectedIds.postValue(updatedUris)
    }

    private fun clickItem(identifier: Identifier, isVideo: Boolean) {
        launch {
            mediaPickerTracker.trackPreview(isVideo, identifier, mediaPickerSetup)
        }
        when (identifier) {
            is Identifier.LocalUri -> {
                mediaUtilsWrapper.getRealPathFromURI(identifier.value.uri)?.let { path ->
                    _onNavigate.postValue(Event(PreviewUrl(path)))
                }
            }
            is Identifier.StockMediaIdentifier -> {
                if (identifier.url != null) {
                    _onNavigate.postValue(Event(PreviewUrl(identifier.url)))
                }
            }
            is Identifier.RemoteId -> {
                site?.let {
                    launch {
                        val media: MediaModel = mediaStore.getSiteMediaWithId(it, identifier.value)
                        _onNavigate.postValue(Event(PreviewMedia(media)))
                    }
                }
            }
            is Identifier.GifMediaIdentifier -> {
                _onNavigate.postValue(Event(PreviewUrl(identifier.largeImageUri.toString())))
            }
            is Identifier.LocalId -> Unit // Do nothing
        }
    }

    fun performInsertAction() {
        val ids = selectedIdentifiers()
        insertIdentifiers(ids)
    }

    private fun insertIdentifiers(ids: List<Identifier>) {
        var job: Job? = null
        job = launch {
            var progressDialogJob: Job? = null
            mediaInsertHandler.insertMedia(ids).flowOn(bgDispatcher).collect {
                when (it) {
                    is InsertModel.Progress -> {
                        progressDialogJob = launch {
                            delay(100)
                            _showProgressDialog.value = Visible(it.title) {
                                job?.cancel()
                                _showProgressDialog.value = Hidden
                            }
                        }
                    }
                    is InsertModel.Error -> {
                        val message = if (it.error.isNotEmpty()) {
                            UiStringResWithParams(
                                    R.string.media_insert_failed_with_reason,
                                    listOf(UiStringText(it.error))
                            )
                        } else {
                            UiStringRes(R.string.media_insert_failed)
                        }
                        _onSnackbarMessage.value = Event(
                                SnackbarMessageHolder(
                                        message
                                )
                        )
                        progressDialogJob?.cancel()
                        job = null
                        _showProgressDialog.value = Hidden
                    }
                    is InsertModel.Success -> {
                        launch {
                            mediaPickerTracker.trackItemsPicked(it.identifiers, mediaPickerSetup)
                        }
                        progressDialogJob?.cancel()
                        job = null
                        _showProgressDialog.value = Hidden
                        if (_searchExpanded.value == true) {
                            _searchExpanded.value = false
                        }
                        _onNavigate.value = Event(MediaNavigationEvent.InsertMedia(it.identifiers))
                    }
                }
            }
        }
    }

    fun performEditAction() {
        val uriList = selectedIdentifiers().mapNotNull { (it as? Identifier.LocalUri)?.value }
        _onNavigate.value = Event(EditMedia(uriList))
    }

    fun clickOnLastTappedIcon() = clickIcon(lastTappedIcon!!)

    private fun clickIcon(icon: MediaPickerIcon) {
        mediaPickerTracker.trackIconClick(icon, mediaPickerSetup)
        if (icon is WpStoriesCapture || icon is CapturePhoto) {
            if (!permissionsHandler.hasPermissionsToAccessPhotos()) {
                _onPermissionsRequested.value = Event(PermissionsRequested.CAMERA)
                lastTappedIcon = icon
                return
            }
        }
        // Do we need tracking here?; review tracking need.

        _onNavigate.postValue(Event(populateIconClickEvent(icon, mediaPickerSetup.canMultiselect)))
    }

    private fun clickOnCamera() {
        when (mediaPickerSetup.cameraSetup) {
            STORIES -> clickIcon(WpStoriesCapture)
            ENABLED -> clickIcon(CapturePhoto)
            HIDDEN -> {
                // Do nothing
            }
        }
    }

    private fun populateIconClickEvent(icon: MediaPickerIcon, canMultiselect: Boolean): IconClickEvent {
        val action: MediaPickerAction = when (icon) {
            is ChooseFromAndroidDevice -> {
                val allowedTypes = icon.allowedTypes
                val (context, types) = when {
                    listOf(IMAGE).containsAll(allowedTypes) -> {
                        Pair(ChooserContext.PHOTO, MimeTypes().getImageTypesOnly())
                    }
                    listOf(VIDEO).containsAll(allowedTypes) -> {
                        Pair(ChooserContext.VIDEO, MimeTypes().getVideoTypesOnly())
                    }
                    listOf(IMAGE, VIDEO).containsAll(allowedTypes) -> {
                        Pair(ChooserContext.PHOTO_OR_VIDEO, MimeTypes().getVideoAndImageTypesOnly())
                    }
                    listOf(AUDIO).containsAll(allowedTypes) -> {
                        Pair(
                                ChooserContext.AUDIO,
                                MimeTypes().getAudioTypesOnly(mediaUtilsWrapper.getSitePlanForMimeTypes(site))
                        )
                    }
                    else -> {
                        Pair(
                                ChooserContext.MEDIA_FILE,
                                MimeTypes().getAllTypes(mediaUtilsWrapper.getSitePlanForMimeTypes(site))
                        )
                    }
                }
                OpenSystemPicker(context, types.toList(), canMultiselect)
            }
            is WpStoriesCapture -> OpenCameraForWPStories(canMultiselect)
            is CapturePhoto -> OpenCameraForPhotos
            is SwitchSource -> {
                SwitchMediaPicker(
                        mediaPickerSetup.copy(
                                primaryDataSource = icon.dataSource,
                                availableDataSources = setOf(),
                                systemPickerEnabled = icon.dataSource == DEVICE,
                                defaultSearchView = icon.dataSource == STOCK_LIBRARY || icon.dataSource == GIF_LIBRARY,
                                cameraSetup = HIDDEN
                        )
                )
            }
        }

        return IconClickEvent(action)
    }

    fun checkStoragePermission(isAlwaysDenied: Boolean) {
        if (!mediaPickerSetup.requiresStoragePermissions) {
            return
        }
        if (permissionsHandler.hasStoragePermission()) {
            _softAskRequest.value = SoftAskRequest(show = false, isAlwaysDenied = isAlwaysDenied)
            if (_domainModel.value?.domainItems.isNullOrEmpty()) {
                refreshData(false)
            }
        } else {
            _softAskRequest.value = SoftAskRequest(show = true, isAlwaysDenied = isAlwaysDenied)
        }
    }

    fun onMenuItemClicked(action: BrowseAction) {
        val icon = when (action) {
            BrowseAction.DEVICE -> SwitchSource(DEVICE)
            BrowseAction.WP_MEDIA_LIBRARY -> SwitchSource(WP_LIBRARY)
            BrowseAction.STOCK_LIBRARY -> SwitchSource(STOCK_LIBRARY)
            BrowseAction.GIF_LIBRARY -> SwitchSource(GIF_LIBRARY)
            BrowseAction.SYSTEM_PICKER -> ChooseFromAndroidDevice(mediaPickerSetup.allowedTypes)
        }
        clickIcon(icon)
    }

    private fun buildSoftAskView(softAskRequest: SoftAskRequest?): SoftAskViewUiModel {
        if (softAskRequest != null && softAskRequest.show) {
            mediaPickerTracker.trackShowPermissionsScreen(mediaPickerSetup, softAskRequest.isAlwaysDenied)
            val appName = "<strong>${resourceProvider.getString(R.string.app_name)}</strong>"
            val label = if (softAskRequest.isAlwaysDenied) {
                val writePermission = ("<strong>${
                    WPPermissionUtils.getPermissionName(
                            resourceProvider,
                            permission.WRITE_EXTERNAL_STORAGE
                    )
                }</strong>")
                val readPermission = ("<strong>${
                    WPPermissionUtils.getPermissionName(
                            resourceProvider,
                            permission.READ_EXTERNAL_STORAGE
                    )
                }</strong>")
                String.format(
                        resourceProvider.getString(R.string.media_picker_soft_ask_permissions_denied),
                        appName,
                        writePermission,
                        readPermission
                )
            } else {
                String.format(
                        resourceProvider.getString(R.string.photo_picker_soft_ask_label),
                        appName
                )
            }
            val allowId = if (softAskRequest.isAlwaysDenied) {
                R.string.button_edit_permissions
            } else {
                R.string.photo_picker_soft_ask_allow
            }
            return SoftAskViewUiModel.Visible(label, UiStringRes(allowId), softAskRequest.isAlwaysDenied)
        } else {
            return SoftAskViewUiModel.Hidden
        }
    }

    fun onSearch(query: String) {
        searchJob?.cancel()
        searchJob = launch(bgDispatcher) {
            delay(300)
            mediaPickerTracker.trackSearch(mediaPickerSetup)
            loadActions.send(LoadAction.Filter(query))
        }
    }

    fun onSearchExpanded() {
        mediaPickerTracker.trackSearchExpanded(mediaPickerSetup)
        _searchExpanded.value = true
    }

    fun onSearchCollapsed() {
        if (!mediaPickerSetup.defaultSearchView) {
            _searchExpanded.value = false
            searchJob?.cancel()
            searchJob = launch(bgDispatcher) {
                mediaPickerTracker.trackSearchCollapsed(mediaPickerSetup)
                loadActions.send(LoadAction.ClearFilter)
            }
        } else {
            _onNavigate.postValue(Event(Exit))
        }
    }

    fun onPullToRefresh() {
        refreshData(true)
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    fun urisSelectedFromSystemPicker(uris: List<UriWrapper>) {
        launch {
            delay(100)
            insertIdentifiers(uris.map { Identifier.LocalUri(it) })
        }
    }

    data class MediaPickerUiState(
        val photoListUiModel: PhotoListUiModel,
        val softAskViewUiModel: SoftAskViewUiModel,
        val fabUiModel: FabUiModel,
        val actionModeUiModel: ActionModeUiModel,
        val searchUiModel: SearchUiModel,
        val isRefreshing: Boolean,
        val browseMenuUiModel: BrowseMenuUiModel,
        val progressDialogUiModel: ProgressDialogUiModel
    )

    sealed class PhotoListUiModel {
        data class Data(val items: List<MediaPickerUiItem>) :
                PhotoListUiModel()

        data class Empty(
            val title: UiString,
            val htmlSubtitle: UiString? = null,
            val image: Int? = null,
            val bottomImage: Int? = null,
            val bottomImageDescription: UiString? = null,
            val isSearching: Boolean = false,
            val retryAction: (() -> Unit)? = null
        ) : PhotoListUiModel()

        object Hidden : PhotoListUiModel()
        object Loading : PhotoListUiModel()
    }

    sealed class SoftAskViewUiModel {
        data class Visible(val label: String, val allowId: UiStringRes, val isAlwaysDenied: Boolean) :
                SoftAskViewUiModel()

        object Hidden : SoftAskViewUiModel()
    }

    data class FabUiModel(val show: Boolean, val action: () -> Unit)

    sealed class ActionModeUiModel {
        data class Visible(
            val actionModeTitle: UiString? = null,
            val editActionUiModel: EditActionUiModel = EditActionUiModel()
        ) : ActionModeUiModel()

        object Hidden : ActionModeUiModel()
    }

    sealed class SearchUiModel {
        object Collapsed : SearchUiModel()
        data class Expanded(val filter: String, val closeable: Boolean = true) : SearchUiModel()
        object Hidden : SearchUiModel()
    }

    data class BrowseMenuUiModel(val shownActions: Set<BrowseAction>) {
        enum class BrowseAction {
            SYSTEM_PICKER, DEVICE, WP_MEDIA_LIBRARY, STOCK_LIBRARY, GIF_LIBRARY
        }
    }

    enum class PermissionsRequested {
        CAMERA, STORAGE
    }

    data class SoftAskRequest(val show: Boolean, val isAlwaysDenied: Boolean)

    data class EditActionUiModel(
        val isVisible: Boolean = false,
        val isCounterBadgeVisible: Boolean = false,
        val counterBadgeValue: Int = 1
    )

    sealed class ProgressDialogUiModel {
        object Hidden : ProgressDialogUiModel()
        data class Visible(val title: Int, val cancelAction: () -> Unit) : ProgressDialogUiModel()
    }
}
