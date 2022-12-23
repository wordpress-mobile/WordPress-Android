package org.wordpress.android.ui.mediapicker.loader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction.FETCH_MEDIA_LIST
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.utils.MimeType.Type
import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.ui.mediapicker.loader.MediaLibraryDataSource.MediaLibraryDataSourceFactory
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Empty
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class MediaLibraryDataSourceTest : BaseUnitTest() {
    @Mock lateinit var mediaStore: MediaStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    private lateinit var mediaLibraryDataSourceFactory: MediaLibraryDataSourceFactory
    private val siteModel = SiteModel()
    private var mediaIdCounter = 1L
    private val errorMessage = "save failed"
    private val actions = mutableListOf<Action<FetchMediaListPayload>>()

    @Before
    fun setUp() {
        mediaLibraryDataSourceFactory = MediaLibraryDataSourceFactory(
                mediaStore,
                dispatcher,
                testDispatcher(),
                networkUtilsWrapper,
                dateTimeUtilsWrapper
        )
        mediaIdCounter = 1L
        actions.clear()
    }

    @Test
    fun `returns failure state with no data when network not available and loadMore is false`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val dataSource = mediaLibraryDataSourceFactory.build(siteModel, setOf())

        val result = dataSource.load(forced = false, loadMore = false, filter = null) as Failure

        assertThat(result.title).isEqualTo(UiStringRes(R.string.no_network_title))
        assertThat(result.htmlSubtitle).isEqualTo(UiStringRes(R.string.no_network_message))
        assertThat(result.image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
        assertThat(result.data).isEmpty()
    }

    @Test
    fun `returns failure state with data when network not available and loadMore is true`() = test {
        val mediaModel = buildMediaModel(10)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(mediaStore.getSiteImages(siteModel)).thenReturn(listOf(mediaModel))

        val dataSource = mediaLibraryDataSourceFactory.build(siteModel, setOf(IMAGE))

        val result = dataSource.load(forced = false, loadMore = true, filter = null) as Failure

        assertThat(result.title).isEqualTo(UiStringRes(R.string.no_network_title))
        assertThat(result.htmlSubtitle).isEqualTo(UiStringRes(R.string.no_network_message))
        assertThat(result.image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
        assertThat(result.data).hasSize(1)
        result.data.assertContains(mediaModel, 0)
    }

    @Test
    fun `triggers load on all the media types`() = test {
        val loadMore = false
        val dataSource = setupDataSource(loadMore, setOf(IMAGE, VIDEO, AUDIO, DOCUMENT))

        dataSource.load(forced = false, loadMore = loadMore, filter = null)

        verify(dispatcher, times(4)).dispatch(any())
        assertEvent(actions[0], loadMore, Type.IMAGE)
        assertEvent(actions[1], loadMore, Type.VIDEO)
        assertEvent(actions[2], loadMore, Type.AUDIO)
        assertEvent(actions[3], loadMore, Type.APPLICATION)
    }

    @Test
    fun `fetches and loads images`() = test {
        fetchAndLoadItem(IMAGE, Type.IMAGE) {
            whenever(mediaStore.getSiteImages(siteModel)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and searches for images with filter`() = test {
        val filter = "filter"
        fetchAndLoadItem(IMAGE, Type.IMAGE, filter) {
            whenever(mediaStore.searchSiteImages(siteModel, filter)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and loads videos`() = test {
        fetchAndLoadItem(VIDEO, Type.VIDEO) {
            whenever(mediaStore.getSiteVideos(siteModel)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and searches for videos with filter`() = test {
        val filter = "filter"
        fetchAndLoadItem(VIDEO, Type.VIDEO, filter) {
            whenever(mediaStore.searchSiteVideos(siteModel, filter)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and loads audios`() = test {
        fetchAndLoadItem(AUDIO, Type.AUDIO) {
            whenever(mediaStore.getSiteAudio(siteModel)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and searches for audios with filter`() = test {
        val filter = "filter"
        fetchAndLoadItem(AUDIO, Type.AUDIO, filter) {
            whenever(mediaStore.searchSiteAudio(siteModel, filter)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and loads documents`() = test {
        fetchAndLoadItem(DOCUMENT, Type.APPLICATION) {
            whenever(mediaStore.getSiteDocuments(siteModel)).thenReturn(listOf(it))
        }
    }

    @Test
    fun `fetches and searches for documents with filter`() = test {
        val filter = "filter"
        fetchAndLoadItem(DOCUMENT, Type.APPLICATION, filter) {
            whenever(mediaStore.searchSiteDocuments(siteModel, filter)).thenReturn(listOf(it))
        }
    }

    private suspend fun fetchAndLoadItem(
        mediaType: MediaType,
        mimeType: Type,
        filter: String? = null,
        init: (mediaModel: MediaModel) -> Unit
    ) {
        val mediaModel = buildMediaModel(10)
        init(mediaModel)
        val loadMore = false
        val hasMore = true
        val dataSource = setupDataSource(hasMore, setOf(mediaType))

        val result = dataSource.load(forced = false, loadMore = loadMore, filter = filter) as Success

        verify(dispatcher).dispatch(any())
        assertEvent(actions[0], loadMore, mimeType)
        assertThat(result.hasMore).isEqualTo(hasMore)
        result.data.assertContains(mediaModel, 0)
    }

    @Test
    fun `orders items from multiple sources correctly`() = test {
        val olderImage = buildMediaModel(10)
        val newerImage = buildMediaModel(30)
        val olderVideo = buildMediaModel(20)
        val newerVideo = buildMediaModel(40)
        whenever(mediaStore.getSiteImages(siteModel)).thenReturn(listOf(olderImage, newerImage))
        whenever(mediaStore.getSiteVideos(siteModel)).thenReturn(listOf(newerVideo, olderVideo))

        val dataSource = setupDataSource(false, setOf(IMAGE, VIDEO))

        val result = dataSource.load(forced = false, loadMore = false, filter = null) as Success

        result.data.assertContains(newerVideo, 0)
        result.data.assertContains(newerImage, 1)
        result.data.assertContains(olderVideo, 2)
        result.data.assertContains(olderImage, 3)
    }

    @Test
    fun `returns failure with empty list when loading fails and not loading more`() = test {
        val dataSource = setupDataSource(false, setOf(IMAGE, VIDEO), isError = true)

        val result = dataSource.load(forced = false, loadMore = false, filter = null) as Failure

        assertThat(result.title).isEqualTo(UiStringRes(R.string.media_loading_failed))
        assertThat(result.htmlSubtitle).isEqualTo(UiStringText(errorMessage))
        assertThat(result.image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
        assertThat(result.data).isEmpty()
    }

    @Test
    fun `returns failure with existing data when loading fails and is loading more`() = test {
        val image = buildMediaModel(10)
        whenever(mediaStore.getSiteImages(siteModel)).thenReturn(listOf(image))

        val dataSource = setupDataSource(false, setOf(IMAGE), isError = true)

        val result = dataSource.load(forced = false, loadMore = true, filter = null) as Failure

        assertThat(result.title).isEqualTo(UiStringRes(R.string.media_loading_failed))
        assertThat(result.htmlSubtitle).isEqualTo(UiStringText(errorMessage))
        assertThat(result.image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
        result.data.assertContains(image, 0)
    }

    @Test
    fun `returns empty when search results are empty`() = test {
        val filter = "filter"
        whenever(mediaStore.searchSiteImages(siteModel, filter)).thenReturn(listOf())
        whenever(mediaStore.searchSiteDocuments(siteModel, filter)).thenReturn(listOf())

        val dataSource = setupDataSource(false, setOf(IMAGE, DOCUMENT))

        val result = dataSource.load(forced = false, loadMore = false, filter = filter) as Empty

        assertThat(result.title).isEqualTo(UiStringRes(R.string.media_empty_search_list))
        assertThat(result.image).isEqualTo(R.drawable.img_illustration_empty_results_216dp)
    }

    private fun setupDataSource(
        hasMore: Boolean,
        allowedTypes: Set<MediaType>,
        isError: Boolean = false
    ): MediaLibraryDataSource {
        val dataSource = mediaLibraryDataSourceFactory.build(siteModel, allowedTypes)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        doAnswer {
            val element = it.getArgument<Action<FetchMediaListPayload>>(0)
            actions.add(element)
            if (!isError) {
                dataSource.onMediaListFetched(OnMediaListFetched(siteModel, hasMore, element.payload.mimeType))
            } else {
                dataSource.onMediaListFetched(
                        OnMediaListFetched(
                                siteModel,
                                MediaError(GENERIC_ERROR, errorMessage),
                                element.payload.mimeType
                        )
                )
            }
        }.whenever(dispatcher).dispatch(any())
        return dataSource
    }

    private fun assertEvent(
        action: Action<FetchMediaListPayload>,
        loadMore: Boolean,
        type: Type
    ): ObjectAssert<SiteModel>? {
        assertThat(action.type).isEqualTo(FETCH_MEDIA_LIST)
        assertThat(action.payload.loadMore).isEqualTo(loadMore)
        assertThat(action.payload.mimeType).isEqualTo(type)
        assertThat(action.payload.number).isEqualTo(24)
        return assertThat(action.payload.site).isEqualTo(siteModel)
    }

    private fun List<MediaItem>.assertContains(mediaModel: MediaModel, position: Int = 0) {
        this[position].let { mediaItem ->
            assertThat(mediaItem.url).isEqualTo(mediaModel.url)
            assertThat(mediaItem.name).isEqualTo(mediaModel.title)
            assertThat(mediaItem.mimeType).isEqualTo(mediaModel.mimeType)
            assertThat(mediaItem.dataModified).isEqualTo(mediaModel.uploadDate.toLong())
        }
    }

    private fun buildMediaModel(date: Long): MediaModel {
        val mediaModel = MediaModel()
        mediaModel.mediaId = mediaIdCounter
        mediaIdCounter += 1
        mediaModel.url = "http://media.jpg"
        mediaModel.title = "media"
        val dateString = date.toString()
        mediaModel.uploadDate = dateString
        mediaModel.mimeType = "image/jpg"
        whenever(dateTimeUtilsWrapper.dateFromIso8601(dateString)).thenReturn(Date(date))
        return mediaModel
    }
}
