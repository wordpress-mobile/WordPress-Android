package org.wordpress.android.ui.mediapicker.loader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.store.StockMediaItem
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.fluxc.store.StockMediaStore.OnStockMediaListFetched
import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.StockMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class StockMediaDataSourceTest : BaseUnitTest() {
    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var stockMediaStore: StockMediaStore

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper
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
        stockMediaDataSource = StockMediaDataSource(
            stockMediaStore,
            testDispatcher(),
            networkUtilsWrapper
        )
    }

    @Test
    fun `returns empty list with filter with less than 2 chars`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val filter = "do"

        val result = stockMediaDataSource.load(forced = false, loadMore = false, filter = filter)

        (result as MediaLoadingResult.Empty).apply {
            assertThat((this.title as UiStringRes).stringRes).isEqualTo(R.string.stock_media_picker_initial_empty_text)
            val subtitle = UiStringResWithParams(
                R.string.stock_media_picker_initial_empty_subtext,
                listOf(UiStringText("<a href='https://pexels.com/'>Pexels</a>"))
            )
            assertThat(this.htmlSubtitle).isEqualTo(subtitle)
            assertThat(this.image).isNull()
        }
        verifyNoInteractions(stockMediaStore)
    }

    @Test
    fun `returns success from store with filter with more than 2 chars`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
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
                    StockMediaIdentifier(url, name, title),
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
    fun `returns empty from store with filter with more than 2 chars`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val filter = "dog"
        val loadMore = false
        val hasMore = false
        whenever(stockMediaStore.fetchStockMedia(filter, loadMore)).thenReturn(
            OnStockMediaListFetched(listOf(), filter, 0, hasMore)
        )
        whenever(stockMediaStore.getStockMedia()).thenReturn(listOf())

        val result = stockMediaDataSource.load(forced = false, loadMore = loadMore, filter = filter)

        (result as MediaLoadingResult.Empty).apply {
            assertThat((this.title as UiStringRes).stringRes).isEqualTo(R.string.media_empty_search_list)
            assertThat(this.htmlSubtitle).isNull()
            assertThat(this.image).isEqualTo(R.drawable.img_illustration_empty_results_216dp)
        }
        verify(stockMediaStore).fetchStockMedia(any(), any())
        verify(stockMediaStore).getStockMedia()
    }

    @Test
    fun `returns error when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val filter = "dog"

        val result = stockMediaDataSource.load(forced = false, loadMore = false, filter = filter)

        (result as MediaLoadingResult.Failure).apply {
            assertThat((this.title as UiStringRes).stringRes).isEqualTo(R.string.no_network_title)
            assertThat(this.htmlSubtitle).isEqualTo(UiStringRes(R.string.no_network_message))
            assertThat(this.image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
        }
        verifyNoInteractions(stockMediaStore)
    }
}
