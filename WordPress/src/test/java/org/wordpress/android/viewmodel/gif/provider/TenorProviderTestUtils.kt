package org.wordpress.android.viewmodel.gif.provider

import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.model.impl.Media
import com.tenor.android.core.model.impl.MediaCollection
import com.tenor.android.core.model.impl.Result
import org.wordpress.android.viewmodel.gif.MutableGifMediaViewModel
import org.wordpress.android.viewmodel.gif.provider.GifProvider.Gif

class TenorProviderTestUtils {
    companion object {
        internal fun createGifResultList(): List<Result> = listOf(
                createGifResultMock("first GIF"),
                createGifResultMock("second GIF"),
                createGifResultMock("third GIF"),
                createGifResultMock("fourth GIF")
        )

        private fun createGifResultMock(mockFilling: String) =
                mock<Result>().apply {
                    whenever(this.id).thenReturn(mockFilling)
                    whenever(this.title).thenReturn(mockFilling)
                    val mediaCollection = createMediaCollectionMock(mockFilling)
                    whenever(this.medias).thenReturn(listOf(mediaCollection))
                }

        private fun createMediaCollectionMock(mockContent: String) =
                mock<MediaCollection>().apply {
                    val nanoGifMedia = createMediaMock("$mockContent gif_nano")
                    val tinyGifMedia = createMediaMock("$mockContent gif_tiny")
                    val gifMedia = createMediaMock("$mockContent gif")

                    whenever(this[MediaCollectionFormat.GIF_NANO]).thenReturn(nanoGifMedia)
                    whenever(this[MediaCollectionFormat.GIF_TINY]).thenReturn(tinyGifMedia)
                    whenever(this[MediaCollectionFormat.GIF]).thenReturn(gifMedia)
                }

        private fun createMediaMock(mockContent: String) =
                mock<Media>().apply { whenever(this.url).thenReturn(mockContent) }

        internal val expectedMediaViewModelCollection = listOf(
                MutableGifMediaViewModel(
                        "first GIF",
                        "first GIF",
                        createExpectedGif("first GIF")
                ),
                MutableGifMediaViewModel(
                        "second GIF",
                        "second GIF",
                        createExpectedGif("second GIF")
                ),
                MutableGifMediaViewModel(
                        "third GIF",
                        "third GIF",
                        createExpectedGif("third GIF")
                ),
                MutableGifMediaViewModel(
                        "fourth GIF",
                        "fourth GIF",
                        createExpectedGif("fourth GIF")
                )
        )

        private fun createExpectedGif(expectedContent: String) = Gif(
                Uri.parse("$expectedContent gif_nano"),
                Uri.parse("$expectedContent gif_tiny"),
                Uri.parse("$expectedContent gif")
        )
    }
}
