package org.wordpress.android.viewmodel.gif.provider

import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.model.impl.Media
import com.tenor.android.core.model.impl.MediaCollection
import com.tenor.android.core.model.impl.Result
import org.wordpress.android.viewmodel.gif.MutableGifMediaViewModel

object TenorProviderTestFixtures {
    internal val mockedTenorResult
        get() = listOf(
                createGifResultMock("first GIF"),
                createGifResultMock("second GIF"),
                createGifResultMock("third GIF"),
                createGifResultMock("fourth GIF")
        )

    internal val expectedGifMediaViewModelCollection = listOf(
            createExpectedGifMediaViewModel("first GIF"),
            createExpectedGifMediaViewModel("second GIF"),
            createExpectedGifMediaViewModel("third GIF"),
            createExpectedGifMediaViewModel("fourth GIF")
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

    private fun createExpectedGifMediaViewModel(expectedContent: String) =
            MutableGifMediaViewModel(
                    expectedContent,
                    Uri.parse("$expectedContent gif_nano"),
                    Uri.parse("$expectedContent gif_tiny"),
                    Uri.parse("$expectedContent gif"),
                    expectedContent
            )
}
