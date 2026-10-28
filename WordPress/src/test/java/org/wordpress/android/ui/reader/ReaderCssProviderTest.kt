package org.wordpress.android.ui.reader

import android.content.Context
import android.content.res.AssetManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.viewmodel.ContextProvider
import java.io.IOException

@RunWith(MockitoJUnitRunner::class)
class ReaderCssProviderTest {
    private val assetManager: AssetManager = mock()
    private val context: Context = mock()
    private val contextProvider: ContextProvider = mock()

    private lateinit var cssProvider: ReaderCssProvider

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.assets).thenReturn(assetManager)
        cssProvider = ReaderCssProvider(contextProvider)
    }

    @Test
    fun `bundled stylesheet is returned`() {
        whenever(assetManager.open(CSS_ASSET_NAME)).thenReturn(CSS.byteInputStream())

        val result = cssProvider.getBaseCss()

        assertThat(result).isEqualTo(CSS)
    }

    @Test
    fun `stylesheet is only read from assets once`() {
        whenever(assetManager.open(CSS_ASSET_NAME)).thenReturn(CSS.byteInputStream())

        repeat(3) { cssProvider.getBaseCss() }

        verify(assetManager).open(CSS_ASSET_NAME)
    }

    @Test
    fun `empty css is returned when the asset cannot be read`() {
        whenever(assetManager.open(CSS_ASSET_NAME)).thenThrow(IOException("missing"))

        val result = cssProvider.getBaseCss()

        assertThat(result).isEmpty()
    }

    @Test
    fun `a failed read is not retried on every render`() {
        whenever(assetManager.open(CSS_ASSET_NAME)).thenThrow(IOException("missing"))

        cssProvider.getBaseCss()
        cssProvider.getBaseCss()

        verify(assetManager).open(CSS_ASSET_NAME)
    }

    companion object {
        private const val CSS_ASSET_NAME = "reader-mobile.css"
        private const val CSS = ".reader-full-post__story-content{color:#2c3338}"
    }
}
