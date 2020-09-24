package org.wordpress.android.ui.mediapicker

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.store.MediaStore.OnStockMediaUploaded
import org.wordpress.android.fluxc.store.StockMediaItem
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.fluxc.store.StockMediaStore.OnStockMediaListFetched
import org.wordpress.android.fluxc.store.StockMediaUploadItem
import org.wordpress.android.test
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.RemoteId
import org.wordpress.android.ui.mediapicker.MediaSource.MediaInsertResult
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE

@InternalCoroutinesApi
class StockMediaDataSourceTest : BaseUnitTest() {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var stockMediaStore: StockMediaStore
    private lateinit var stockMediaDataSource: StockMediaDataSource
    private val url = "wordpress://url"
    private val title = "title"
    private val name = "name"
    private val thumbnail = "image.jpg"
    private val stockMediaItem = StockMediaItem(
            "id",
            name,
            title,
            url,
            "123",
            thumbnail
    )

    @Before
    fun setUp() {
        stockMediaDataSource = StockMediaDataSource(site, stockMediaStore, TEST_DISPATCHER)
    }

    @Test
    fun `returns empty list with filter with less than 2 chars`() = test {
        val filter = "do"

        val result = stockMediaDataSource.load(forced = false, loadMore = false, filter = filter)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).isEmpty()
            assertThat(this.hasMore).isFalse()
        }
        verifyZeroInteractions(stockMediaStore)
    }

    @Test
    fun `returns success from store with filter with more than 2 chars`() = test {
        val filter = "dog"
        val loadMore = false
        val hasMore = true
        whenever(stockMediaStore.fetchStockMedia(filter, loadMore)).thenReturn(
                OnStockMediaListFetched(listOf(StockMediaModel()), filter, 1, hasMore)
        )
        whenever(stockMediaStore.getStockMedia()).thenReturn(listOf(stockMediaItem))

        val result = stockMediaDataSource.load(forced = false, loadMore = loadMore, filter = filter)

        (result as MediaLoadingResult.Success).apply {
            assertThat(this.data).hasSize(1)
            assertThat(this.data).containsExactly(
                    MediaItem(
                            Identifier.StockMediaIdentifier(url, name, title),
                            url,
                            name,
                            IMAGE,
                            null,
                            123
                    )
            )
            assertThat(this.hasMore).isTrue()
        }
        verify(stockMediaStore).fetchStockMedia(any(), any())
        verify(stockMediaStore).getStockMedia()
    }

    @Test
    fun `uploads media on insert`() = test {
        val itemToInsert = Identifier.StockMediaIdentifier(url, name, title)
        val insertedMediaModel = MediaModel()
        val mediaId: Long = 10
        insertedMediaModel.mediaId = mediaId
        whenever(stockMediaStore.performUploadStockMedia(any(), any())).thenReturn(OnStockMediaUploaded(site, listOf(
                insertedMediaModel
        )))

        val result = stockMediaDataSource.insert(listOf(itemToInsert))

        (result as MediaInsertResult.Success).apply {
            assertThat(this.identifiers).containsExactly(RemoteId(mediaId))
        }
        verify(stockMediaStore).performUploadStockMedia(site, listOf(StockMediaUploadItem(name, title, url)))
    }
}
