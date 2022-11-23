package org.wordpress.android.fluxc.store

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
import org.wordpress.android.fluxc.persistence.StockMediaSqlUtils
import org.wordpress.android.fluxc.store.StockMediaStore.FetchedStockMediaListPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class StockMediaStoreTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var restClient: StockMediaRestClient
    @Mock lateinit var sqlUtils: StockMediaSqlUtils
    @Mock lateinit var mediaStore: MediaStore
    private lateinit var store: StockMediaStore
    private val stockMediaModel = StockMediaModel()
    private lateinit var stockMediaItem: StockMediaItem
    private val id = "id"
    private val name = "name"
    private val title = "title"
    private val date = "date"
    private val url = "url"
    private val thumbnail = "thumbnail"

    @Before
    fun setUp() {
        store = StockMediaStore(
                dispatcher,
                restClient,
                initCoroutineEngine(),
                sqlUtils,
                mediaStore
        )
        stockMediaModel.id = id
        stockMediaModel.name = name
        stockMediaModel.title = title
        stockMediaModel.date = date
        stockMediaModel.url = url
        stockMediaModel.thumbnail = thumbnail
        stockMediaItem = StockMediaItem(id, name, title, url, date, thumbnail)
    }

    @Test
    fun `fetches first page with load more when next page available`() = test {
        val filter = "filter"
        val mediaList = listOf(stockMediaModel)
        whenever(restClient.searchStockMedia(filter, 0, StockMediaStore.PAGE_SIZE)).thenReturn(
                FetchedStockMediaListPayload(mediaList, filter, 1, true)
        )

        val result = store.fetchStockMedia(filter, false)

        verify(sqlUtils).insert(0, 1, listOf(stockMediaItem))

        assertThat(result.searchTerm).isEqualTo(filter)
        assertThat(result.canLoadMore).isTrue()
        assertThat(result.nextPage).isEqualTo(1)
        assertThat(result.mediaList).isEqualTo(mediaList)
    }

    @Test
    fun `fetches first page without load more when next page not available`() = test {
        val filter = "filter"
        val mediaList = listOf(stockMediaModel)
        whenever(restClient.searchStockMedia(filter, 0, StockMediaStore.PAGE_SIZE)).thenReturn(
                FetchedStockMediaListPayload(mediaList, filter, 0, false)
        )

        val result = store.fetchStockMedia(filter, false)

        verify(sqlUtils).insert(0, null, listOf(stockMediaItem))

        assertThat(result.searchTerm).isEqualTo(filter)
        assertThat(result.canLoadMore).isFalse()
        assertThat(result.nextPage).isEqualTo(0)
        assertThat(result.mediaList).isEqualTo(mediaList)
    }

    @Test
    fun `fetches next page when available when loadMore is true`() = test {
        val filter = "filter"
        val mediaList = listOf(stockMediaModel)
        val nextPage = 2
        whenever(sqlUtils.getNextPage()).thenReturn(nextPage)
        whenever(restClient.searchStockMedia(filter, nextPage, StockMediaStore.PAGE_SIZE)).thenReturn(
                FetchedStockMediaListPayload(mediaList, filter, 0, false)
        )

        val result = store.fetchStockMedia(filter, true)

        verify(sqlUtils).insert(2, null, listOf(stockMediaItem))
        verify(sqlUtils, never()).clear()

        assertThat(result.searchTerm).isEqualTo(filter)
        assertThat(result.canLoadMore).isFalse()
        assertThat(result.nextPage).isEqualTo(0)
        assertThat(result.mediaList).isEqualTo(mediaList)
    }

    @Test
    fun `fetches first page when next page not available and loadMore is true`() = test {
        val filter = "filter"
        val mediaList = listOf(stockMediaModel)
        whenever(sqlUtils.getNextPage()).thenReturn(null)
        whenever(restClient.searchStockMedia(filter, 0, StockMediaStore.PAGE_SIZE)).thenReturn(
                FetchedStockMediaListPayload(mediaList, filter, 0, false)
        )

        val result = store.fetchStockMedia(filter, true)

        inOrder(sqlUtils) {
            verify(sqlUtils).clear()
            verify(sqlUtils).insert(0, null, listOf(stockMediaItem))
        }

        assertThat(result.searchTerm).isEqualTo(filter)
        assertThat(result.canLoadMore).isFalse()
        assertThat(result.nextPage).isEqualTo(0)
        assertThat(result.mediaList).isEqualTo(mediaList)
    }

    @Test
    fun `first page fetch clears old data`() = test {
        val filter = "filter"
        val mediaList = listOf(stockMediaModel)
        whenever(restClient.searchStockMedia(filter, 0, StockMediaStore.PAGE_SIZE)).thenReturn(
                FetchedStockMediaListPayload(mediaList, filter, 1, true)
        )

        store.fetchStockMedia(filter, false)

        verify(sqlUtils).clear()
    }
}
