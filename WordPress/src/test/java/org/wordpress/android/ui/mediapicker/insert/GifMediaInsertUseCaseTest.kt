package org.wordpress.android.ui.mediapicker.insert

import android.content.Context
import android.webkit.MimeTypeMap
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalId
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Progress
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Success
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MimeTypeMapUtilsWrapper
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper

@InternalCoroutinesApi
class GifMediaInsertUseCaseTest : BaseUnitTest() {
    @Mock lateinit var context: Context
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var uriWrapper: UriWrapper
    @Mock lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper
    @Mock lateinit var mimeTypeMapUtilsWrapper: MimeTypeMapUtilsWrapper
    @Mock lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper

    private lateinit var gifMediaInsertUseCase: GifMediaInsertUseCase

    @Before
    fun setUp() {
        gifMediaInsertUseCase = GifMediaInsertUseCase(
                context,
                site,
                dispatcher,
                TEST_DISPATCHER,
                wpMediaUtilsWrapper,
                fluxCUtilsWrapper,
                mimeTypeMapUtilsWrapper
        )
    }

    @Test
    fun `uploads media on insert`() = test {
        whenever(uriWrapper.toString()).thenReturn("https://sampleutl.org")

        val itemToInsert = GifMediaIdentifier(uriWrapper, null)
        val insertedMediaModel = MediaModel().apply { id = 100 }

        whenever(wpMediaUtilsWrapper.fetchMediaToUriWrapper(any())).thenReturn(mock())
        whenever(mimeTypeMapUtilsWrapper.getFileExtensionFromUrl(any())).thenReturn("png")
        val mimeTypeMap: MimeTypeMap = mock()
        whenever(mimeTypeMap.getMimeTypeFromExtension(any())).thenReturn("image/png")
        whenever(mimeTypeMapUtilsWrapper.getSingleton()).thenReturn(mimeTypeMap)
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(
                anyOrNull(),
                anyOrNull(),
                anyInt()
        )).thenReturn(insertedMediaModel)

        val result = gifMediaInsertUseCase.insert(listOf(itemToInsert)).toList()

        Assertions.assertThat(result[0] is Progress).isTrue()
        (result[1] as Success).apply {
            Assertions.assertThat(this.identifiers).hasSize(1)
            (this.identifiers[0] as LocalId).apply {
                Assertions.assertThat(this.value).isEqualTo(insertedMediaModel.id)
            }
        }
    }
}
