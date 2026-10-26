package org.wordpress.android.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener

@ExperimentalCoroutinesApi
class ErrorManagedWebViewClientTest : BaseUnitTest() {
    @Mock
    private lateinit var listener: ErrorManagedWebViewClientListener

    private lateinit var webViewClient: ErrorManagedWebViewClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        webViewClient = ErrorManagedWebViewClient(listener)
    }

    @Test
    fun `when the render process is gone, then the error is reported and the app is kept alive`() {
        val handled = webViewClient.onRenderProcessGone(mock(), mock())

        assertThat(handled).isTrue()
        verify(listener).onWebViewReceivedError()
    }

    @Test
    fun `when the render process is gone, then a page finished is not reported as a load`() {
        webViewClient.onRenderProcessGone(mock(), mock())

        webViewClient.onPageFinished(mock(), "https://example.com")

        verify(listener, never()).onWebViewPageLoaded()
    }
}
