package org.wordpress.android.ui.mediapicker.insert

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.OnStockMediaUploaded
import org.wordpress.android.fluxc.store.StockMediaItem
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.fluxc.store.StockMediaUploadItem
import org.wordpress.android.test
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.RemoteId
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel

@InternalCoroutinesApi
class StockMediaInsertUseCaseTest : BaseUnitTest() {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var stockMediaStore: StockMediaStore
    private lateinit var stockMediaInsertUseCase: StockMediaInsertUseCase
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
        stockMediaInsertUseCase = StockMediaInsertUseCase(site, stockMediaStore)
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

        val result = stockMediaInsertUseCase.insert(listOf(itemToInsert)).toList()

        assertThat(result[0] is InsertModel.Progress).isTrue()
        (result[1] as InsertModel.Success).apply {
            assertThat(this.identifiers).containsExactly(RemoteId(mediaId))
        }
        verify(stockMediaStore).performUploadStockMedia(site, listOf(StockMediaUploadItem(name, title, url)))
    }
}
