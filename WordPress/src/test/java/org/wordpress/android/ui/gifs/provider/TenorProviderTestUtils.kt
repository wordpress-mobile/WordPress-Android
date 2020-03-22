package org.wordpress.android.ui.gifs.provider

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.model.impl.Result
import org.wordpress.android.ui.gifs.provider.GifProvider.Gif

class TenorProviderTestUtils {
    companion object {
        internal fun createGifResultList(): List<Result> = listOf(
                createGifResultMock("first GIF"),
                createGifResultMock("second GIF"),
                createGifResultMock("third GIF"),
                createGifResultMock("fourth GIF")
        )

        private fun createGifResultMock(url: String): Result =
                mock<Result>().apply { whenever(this.url).thenReturn(url) }

        internal val expectedGifList = listOf(
                Gif("first GIF"),
                Gif("second GIF"),
                Gif("third GIF"),
                Gif("fourth GIF")
        )
    }
}
