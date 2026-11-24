package org.wordpress.android.ui.media.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.ui.media.VideoLoader
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class MediaLibraryViewModelTest : BaseUnitTest() {
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var dispatcher: Dispatcher
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var mediaStore: MediaStore
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var siteStore: SiteStore
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var resourceProvider: ResourceProvider
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var uploadServiceFacade: UploadServiceFacade
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var deleteController: MediaDeleteServiceController
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var uploader: MediaLibraryUploader
    @Mock(strictness = Mock.Strictness.LENIENT) lateinit var videoLoader: VideoLoader

    private val site = SiteModel().apply {
        id = SITE_LOCAL_ID
        siteId = 123L
    }
    private var activeViewModel: MediaLibraryViewModel? = null

    @Before
    fun setUp() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(resourceProvider.getString(any())).thenReturn("string")
        whenever(mediaStore.getAllSiteMedia(any())).thenReturn(emptyList())
        whenever(mediaStore.getSiteAudio(any())).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = MediaLibraryViewModel(
        savedStateHandle = SavedStateHandle(mapOf(WordPress.SITE to site)),
        dispatcher = dispatcher,
        mediaStore = mediaStore,
        siteStore = siteStore,
        resourceProvider = resourceProvider,
        networkUtilsWrapper = networkUtilsWrapper,
        buildConfigWrapper = buildConfigWrapper,
        uploadServiceFacade = uploadServiceFacade,
        deleteController = deleteController,
        uploader = uploader,
        videoLoader = videoLoader,
        bgDispatcher = testDispatcher()
    ).also { activeViewModel = it }

    private fun dispatchedActions(): List<Action<*>> {
        val captor = argumentCaptor<Action<*>>()
        verify(dispatcher, atLeastOnce()).dispatch(captor.capture())
        return captor.allValues
    }

    private fun fetchListActions() = dispatchedActions().filter { it.type == MediaAction.FETCH_MEDIA_LIST }

    private fun media(localId: Int, uploadState: MediaUploadState, mimeType: String = "image/jpeg") =
        MediaModel(SITE_LOCAL_ID, 0L).apply {
            id = localId
            setUploadState(uploadState)
            this.mimeType = mimeType
            fileName = "file$localId.jpg"
            url = "https://example.com/file$localId.jpg"
        }

    @Test
    fun `opening the library fetches the first page of all media`() = test {
        createViewModel()

        val fetches = fetchListActions()
        assertThat(fetches).hasSize(1)
        val payload = fetches.single().payload as FetchMediaListPayload
        assertThat(payload.loadMore).isFalse()
        assertThat(payload.mimeType).isNull()
    }

    @Test
    fun `opening the library offline shows the network error and doesn't fetch`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val viewModel = createViewModel()

        verify(dispatcher, never()).dispatch(any())
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.NETWORK_ERROR)
    }

    @Test
    fun `an error on one filter doesn't show on a filter that already fetched`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, false, null))
        viewModel.onFilterSelected(MediaFilter.DOCUMENTS)
        viewModel.onMediaListFetched(
            OnMediaListFetched(site, MediaError(MediaErrorType.GENERIC_ERROR), MimeType.Type.APPLICATION)
        )

        viewModel.onFilterSelected(MediaFilter.VIDEOS)

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.NO_CONTENT)
    }

    @Test
    fun `a refresh during a load-more fetches the first page once the load-more lands`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))
        viewModel.onLoadMore()

        viewModel.onRefresh()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        val firstPages = fetchListActions().filter { !(it.payload as FetchMediaListPayload).loadMore }
        assertThat(firstPages).hasSize(2)
    }

    @Test
    fun `refreshing all media makes a fully fetched filter fetch again`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))
        viewModel.onFilterSelected(MediaFilter.IMAGES)
        viewModel.onMediaListFetched(OnMediaListFetched(site, false, MimeType.Type.IMAGE))
        viewModel.onFilterSelected(MediaFilter.ALL)

        viewModel.onRefresh()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))
        viewModel.onFilterSelected(MediaFilter.IMAGES)

        val imageFetches = fetchListActions().filter {
            (it.payload as FetchMediaListPayload).mimeType == MimeType.Type.IMAGE
        }
        assertThat(imageFetches).hasSize(2)
        assertThat(viewModel.uiState.value.canLoadMore).isTrue()
    }

    @Test
    fun `selecting a filter fetches it only the first time`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        viewModel.onFilterSelected(MediaFilter.IMAGES)
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, MimeType.Type.IMAGE))
        viewModel.onFilterSelected(MediaFilter.ALL)
        viewModel.onFilterSelected(MediaFilter.IMAGES)

        val imageFetches = fetchListActions().filter {
            (it.payload as FetchMediaListPayload).mimeType == MimeType.Type.IMAGE
        }
        assertThat(imageFetches).hasSize(1)
    }

    @Test
    fun `searching reads the local store without fetching`() = test {
        val match = media(1, MediaUploadState.UPLOADED)
        whenever(mediaStore.searchSiteMedia(any(), eq("cat"))).thenReturn(listOf(match))
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        viewModel.onSearchOpen()
        viewModel.onSearchQueryChanged("cat")
        viewModel.onRefresh()

        assertThat(viewModel.uiState.value.items.map { it.localId }).containsExactly(1)
        assertThat(viewModel.uiState.value.canLoadMore).isFalse()
        assertThat(fetchListActions()).hasSize(1)
    }

    @Test
    fun `fetching every page stops paging`() = test {
        val viewModel = createViewModel()

        viewModel.onMediaListFetched(OnMediaListFetched(site, false, null))
        viewModel.onLoadMore()

        assertThat(viewModel.uiState.value.canLoadMore).isFalse()
        assertThat(fetchListActions()).hasSize(1)
    }

    @Test
    fun `a fetch error stops paging until a successful refresh`() = test {
        val viewModel = createViewModel()

        viewModel.onMediaListFetched(OnMediaListFetched(site, MediaError(MediaErrorType.GENERIC_ERROR), null))
        assertThat(viewModel.uiState.value.canLoadMore).isFalse()
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.GENERIC_ERROR)

        viewModel.onRefresh()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))
        assertThat(viewModel.uiState.value.canLoadMore).isTrue()
    }

    @Test
    fun `a failed fetch for another filter doesn't stop paging the current one`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        viewModel.onMediaListFetched(
            OnMediaListFetched(site, MediaError(MediaErrorType.GENERIC_ERROR), MimeType.Type.IMAGE)
        )

        assertThat(viewModel.uiState.value.canLoadMore).isTrue()
        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.NO_CONTENT)
    }

    @Test
    fun `another filter's result doesn't end the current filter's loading state`() = test {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(MediaFilter.DOCUMENTS)
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.LOADING)
    }

    @Test
    fun `tapping the selected filter keeps its error state`() = test {
        val viewModel = createViewModel()
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))
        viewModel.onRefresh()
        viewModel.onMediaListFetched(OnMediaListFetched(site, MediaError(MediaErrorType.GENERIC_ERROR), null))

        viewModel.onFilterSelected(MediaFilter.ALL)

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.GENERIC_ERROR)
    }

    @Test
    fun `closing a restored search fetches the first page it skipped`() = test {
        val viewModel = MediaLibraryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WordPress.SITE to site,
                    "media_library_search_active" to true,
                    "media_library_search_query" to "cat"
                )
            ),
            dispatcher = dispatcher,
            mediaStore = mediaStore,
            siteStore = siteStore,
            resourceProvider = resourceProvider,
            networkUtilsWrapper = networkUtilsWrapper,
            buildConfigWrapper = buildConfigWrapper,
            uploadServiceFacade = uploadServiceFacade,
            deleteController = deleteController,
            uploader = uploader,
            videoLoader = videoLoader,
            bgDispatcher = testDispatcher()
        ).also { activeViewModel = it }
        whenever(mediaStore.searchSiteMedia(any(), any())).thenReturn(emptyList())

        viewModel.onSearchClose()

        assertThat(fetchListActions()).hasSize(1)
    }

    @Test
    fun `an authorization error shows the permission empty state`() = test {
        val viewModel = createViewModel()

        viewModel.onMediaListFetched(
            OnMediaListFetched(site, MediaError(MediaErrorType.AUTHORIZATION_REQUIRED), null)
        )

        assertThat(viewModel.uiState.value.emptyState).isEqualTo(MediaEmptyState.PERMISSION_ERROR)
    }

    @Test
    fun `the audio tab appears once the site has audio`() = test {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.filters).doesNotContain(MediaFilter.AUDIO)

        whenever(mediaStore.getSiteAudio(any())).thenReturn(listOf(media(1, MediaUploadState.UPLOADED, "audio/mp3")))
        viewModel.onMediaListFetched(OnMediaListFetched(site, true, null))

        assertThat(viewModel.uiState.value.filters).contains(MediaFilter.AUDIO)
    }

    @Test
    fun `a long press starts a selection that taps then extend`() = test {
        whenever(mediaStore.getAllSiteMedia(any())).thenReturn(
            listOf(media(1, MediaUploadState.UPLOADED), media(2, MediaUploadState.UPLOADED))
        )
        val viewModel = createViewModel()

        viewModel.onItemLongClick(2)
        viewModel.onItemClick(1)

        assertThat(viewModel.uiState.value.selectedIds).containsExactly(2, 1)
    }

    @Test
    fun `media being deleted can't be selected`() = test {
        whenever(mediaStore.getAllSiteMedia(any())).thenReturn(listOf(media(1, MediaUploadState.DELETING)))
        val viewModel = createViewModel()

        viewModel.onItemLongClick(1)

        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
    }

    @Test
    fun `confirming a delete removes local media and queues remote media`() = test {
        val local = media(1, MediaUploadState.FAILED)
        val remote = media(2, MediaUploadState.UPLOADED)
        whenever(mediaStore.getAllSiteMedia(any())).thenReturn(listOf(local, remote))
        whenever(mediaStore.getMediaWithLocalId(1)).thenReturn(local)
        whenever(mediaStore.getMediaWithLocalId(2)).thenReturn(remote)
        val viewModel = createViewModel()
        viewModel.onItemLongClick(1)
        viewModel.onItemClick(2)

        viewModel.onDeleteSelectedClick()
        assertThat(viewModel.uiState.value.showDeleteConfirmation).isTrue()
        viewModel.onConfirmDelete()

        val types = dispatchedActions().map { it.type }
        assertThat(types).contains(MediaAction.REMOVE_MEDIA, MediaAction.UPDATE_MEDIA)
        assertThat(remote.uploadState).isEqualTo(MediaUploadState.DELETING.toString())
        verify(deleteController).start(eq(site), eq(listOf(remote)))
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `deleting remote media offline does nothing`() = test {
        val remote = media(2, MediaUploadState.UPLOADED)
        whenever(mediaStore.getMediaWithLocalId(2)).thenReturn(remote)
        val viewModel = createViewModel()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onItemTrashClick(2)

        verify(deleteController, never()).start(any(), anyOrNull())
        assertThat(remote.uploadState).isEqualTo(MediaUploadState.UPLOADED.toString())
    }

    @Test
    fun `tapping media opens its details with the ids of the visible media`() = test {
        val first = media(1, MediaUploadState.UPLOADED)
        val second = media(2, MediaUploadState.UPLOADED)
        whenever(mediaStore.getAllSiteMedia(any())).thenReturn(listOf(first, second))
        whenever(mediaStore.getMediaWithLocalId(2)).thenReturn(second)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onItemClick(2)
            val event = awaitItem() as MediaLibraryEvent.OpenMediaSettings
            assertThat(event.media).isEqualTo(second)
            assertThat(event.mediaIds).containsExactly("1", "2")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val SITE_LOCAL_ID = 1
    }
}
