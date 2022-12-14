package org.wordpress.android.ui.mediapicker.loader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.loader.DeviceMediaLoader.DeviceMediaItem
import org.wordpress.android.ui.mediapicker.loader.DeviceMediaLoader.DeviceMediaList
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.UriWrapper

@ExperimentalCoroutinesApi
class DeviceListBuilderTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var deviceMediaLoader: DeviceMediaLoader
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var site: SiteModel
    @Mock lateinit var uri1: UriWrapper
    @Mock lateinit var uri2: UriWrapper
    @Mock lateinit var uri3: UriWrapper
    private lateinit var deviceListBuilder: DeviceListBuilder
    private lateinit var newestItem: DeviceMediaItem
    private lateinit var middleItem: DeviceMediaItem
    private lateinit var oldestItem: DeviceMediaItem
    private val pageSize = 1
    private val mediaType = IMAGE
    private val mediaMimeType = "image/png"
    private val documentMimeType = "application/pdf"

    @Before
    fun setUp() {
        newestItem = DeviceMediaItem(uri1, "Newest item", 10)
        middleItem = DeviceMediaItem(uri2, "Middle item", 9)
        oldestItem = DeviceMediaItem(uri3, "Oldest item", 8)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(any(), any())).thenReturn(true)
    }

    @Test
    fun `media - loads first page and has more is false when next item is missing`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.hasMore).isFalse()
            assertMediaItem(newestItem)
        }
    }

    @Test
    fun `media - loads first page and has more is true when next item is present`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.hasMore).isTrue()
            assertMediaItem(newestItem)
        }
    }

    @Test
    fun `media - loads second page`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        setupMedia(mediaType, middleItem.dateModified, DeviceMediaList(listOf(middleItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)

        deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val result = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(2)
            assertThat(this.hasMore).isFalse()
            assertMediaItem(newestItem, position = 0)
            assertMediaItem(middleItem, position = 1)
        }
    }

    @Test
    fun `media - loads second page with has more == true`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        setupMedia(mediaType, middleItem.dateModified, DeviceMediaList(listOf(middleItem), oldestItem.dateModified))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)

        deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val result = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(2)
            assertThat(this.hasMore).isTrue()
            assertMediaItem(newestItem, position = 0)
            assertMediaItem(middleItem, position = 1)
        }
    }

    @Test
    fun `media - loads first page when isMimeTypeSupportedBySitePlan is true`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(any(), any())).thenReturn(true)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertMediaItem(newestItem)
        }
    }

    @Test
    fun `media - loads first page with no results when isMimeTypeSupportedBySitePlan is false`() = test {
        setUp(setOf(mediaType))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(mediaMimeType)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(any(), any())).thenReturn(false)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).isEmpty()
        }
    }

    @Test
    fun `document - loads first page and has more is false when next item is missing`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.hasMore).isFalse()
            assertDocumentItem(newestItem)
        }
    }

    @Test
    fun `document - loads first page and has more is true when next item is present`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.hasMore).isTrue()
            assertDocumentItem(newestItem)
        }
    }

    @Test
    fun `document - loads second page`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        setupDocuments(middleItem.dateModified, DeviceMediaList(listOf(middleItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)

        deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val result = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(2)
            assertThat(this.hasMore).isFalse()
            assertDocumentItem(newestItem, position = 0)
            assertDocumentItem(middleItem, position = 1)
        }
    }

    @Test
    fun `document - loads second page with has more == true`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        setupDocuments(middleItem.dateModified, DeviceMediaList(listOf(middleItem), oldestItem.dateModified))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)

        deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val result = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(2)
            assertThat(this.hasMore).isTrue()
            assertDocumentItem(newestItem, position = 0)
            assertDocumentItem(middleItem, position = 1)
        }
    }

    @Test
    fun `loads first image and skips document when the next image is newer than the document`() = test {
        setUp(setOf(IMAGE, DOCUMENT))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem), middleItem.dateModified))
        setupMedia(mediaType, middleItem.dateModified, DeviceMediaList(listOf(middleItem)))
        whenever(deviceMediaLoader.getMimeType(newestItem.uri)).thenReturn(mediaMimeType)
        whenever(deviceMediaLoader.getMimeType(middleItem.uri)).thenReturn(mediaMimeType)
        setupDocuments(null, DeviceMediaList(listOf(oldestItem)))
        whenever(deviceMediaLoader.getMimeType(oldestItem.uri)).thenReturn(documentMimeType)

        val firstPage = deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val secondPage = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (firstPage as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.hasMore).isTrue()
            assertMediaItem(newestItem, position = 0)
        }

        (secondPage as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(3)
            assertThat(this.hasMore).isFalse()
            assertMediaItem(newestItem, position = 0)
            assertMediaItem(middleItem, position = 1)
            assertDocumentItem(oldestItem, position = 2)
        }
    }

    @Test
    fun `loads first image and document when the next image is older`() = test {
        setUp(setOf(IMAGE, DOCUMENT))
        setupMedia(mediaType, null, DeviceMediaList(listOf(newestItem), oldestItem.dateModified))
        setupMedia(mediaType, oldestItem.dateModified, DeviceMediaList(listOf(oldestItem)))
        whenever(deviceMediaLoader.getMimeType(newestItem.uri)).thenReturn(mediaMimeType)
        whenever(deviceMediaLoader.getMimeType(oldestItem.uri)).thenReturn(mediaMimeType)
        setupDocuments(null, DeviceMediaList(listOf(middleItem)))
        whenever(deviceMediaLoader.getMimeType(middleItem.uri)).thenReturn(documentMimeType)

        val firstPage = deviceListBuilder.load(forced = false, loadMore = false, filter = null)
        val secondPage = deviceListBuilder.load(forced = false, loadMore = true, filter = null)

        (firstPage as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(2)
            assertThat(this.hasMore).isTrue()
            assertMediaItem(newestItem, position = 0)
            assertDocumentItem(middleItem, position = 1)
        }

        (secondPage as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(3)
            assertThat(this.hasMore).isFalse()
            assertMediaItem(newestItem, position = 0)
            assertDocumentItem(middleItem, position = 1)
            assertMediaItem(oldestItem, position = 2)
        }
    }

    @Test
    fun `document - loads first page when isMimeTypeSupportedBySitePlan is true`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(any(), any())).thenReturn(true)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertDocumentItem(newestItem)
        }
    }

    @Test
    fun `document - loads first page with no results when isMimeTypeSupportedBySitePlan is false`() = test {
        setUp(setOf(DOCUMENT))
        setupDocuments(null, DeviceMediaList(listOf(newestItem)))
        whenever(deviceMediaLoader.getMimeType(any())).thenReturn(documentMimeType)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(any(), any())).thenReturn(false)

        val result = deviceListBuilder.load(forced = false, loadMore = false, filter = null)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).isEmpty()
        }
    }

    private fun setupMedia(type: MediaType, nextTimestamp: Long?, results: DeviceMediaList) {
        whenever(deviceMediaLoader.loadMedia(type, null, pageSize, nextTimestamp)).thenReturn(
                results
        )
    }

    private fun setupDocuments(nextTimestamp: Long?, results: DeviceMediaList) {
        whenever(deviceMediaLoader.loadDocuments(null, pageSize, nextTimestamp)).thenReturn(
                results
        )
    }

    private fun setUp(allowedTypes: Set<MediaType>) {
        deviceListBuilder = DeviceListBuilder(
                localeManagerWrapper,
                deviceMediaLoader,
                mediaUtilsWrapper,
                site,
                coroutinesTestRule.testDispatcher,
                allowedTypes,
                pageSize
        )
    }

    private fun MediaLoadingResult.Success.assertMediaItem(
        deviceMediaItem: DeviceMediaItem,
        position: Int = 0
    ) = assertItem(deviceMediaItem, mediaType, position, mediaMimeType)

    private fun MediaLoadingResult.Success.assertDocumentItem(
        deviceMediaItem: DeviceMediaItem,
        position: Int = 0
    ) = assertItem(deviceMediaItem, DOCUMENT, position, documentMimeType)

    private fun MediaLoadingResult.Success.assertItem(
        deviceMediaItem: DeviceMediaItem,
        mediaType: MediaType,
        position: Int = 0,
        mimeType: String? = null
    ) {
        this.data[position].let { mediaItem ->
            assertThat((mediaItem.identifier as LocalUri).value).isEqualTo(deviceMediaItem.uri)
            assertThat(mediaItem.name).isEqualTo(deviceMediaItem.title)
            assertThat(mediaItem.dataModified).isEqualTo(deviceMediaItem.dateModified)
            assertThat(mediaItem.type).isEqualTo(mediaType)
            mimeType?.let {
                assertThat(mediaItem.mimeType).isEqualTo(it)
            }
        }
    }
}
