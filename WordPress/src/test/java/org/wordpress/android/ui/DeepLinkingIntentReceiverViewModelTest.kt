package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow

class DeepLinkingIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock lateinit var editorLinkHandler: EditorLinkHandler
    @Mock lateinit var statsLinkHandler: StatsLinkHandler
    @Mock lateinit var startLinkHandler: StartLinkHandler
    @Mock lateinit var readerLinkHandler: ReaderLinkHandler
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var serverTrackingHandler: ServerTrackingHandler
    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel
    private val startUrl = buildUri("wordpress.com", "start")
    private val postUrl = buildUri("wordpress.com", "post")
    private val statsUrl = buildUri("wordpress.com", "stats")

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = DeepLinkingIntentReceiverViewModel(
                TEST_DISPATCHER,
                editorLinkHandler,
                statsLinkHandler,
                startLinkHandler,
                readerLinkHandler,
                deepLinkUriUtils,
                serverTrackingHandler
        )
        whenever(startLinkHandler.isStartUrl(startUrl)).thenReturn(true)
        whenever(editorLinkHandler.isEditorUrl(postUrl)).thenReturn(true)
    }

    @Test
    fun `should not handle WPcom URL`() {
        val uri = buildUri("wordpress.com", "bar")

        val shouldHandleUri = viewModel.handleUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `should not handle bar non-mobile URL`() {
        val uri = buildUri("public-api.wordpress.com", "bar")

        val shouldHandleUri = viewModel.handleUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `magic login URL opens the URI in the browser without redirect parameter`() {
        val uri = buildUri("public-api.wordpress.com", "mbar")
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        val barUri = buildUri("public-api.wordpress.com", "bar")
        whenever(uri.copy("bar")).thenReturn(barUri)

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `create site mbar URL triggers the Site Creation flow`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val firstRedirect = buildUri("wordpress.com", "wp-login.php", "redirect_to...")

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getUriFromQueryParameter(firstRedirect, "redirect_to")).thenReturn(startUrl)
        whenever(startLinkHandler.buildNavigateAction()).thenReturn(StartCreateSiteFlow)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(StartCreateSiteFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `wp-login mbar URL redirects user to browser with missing second redirect`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "wp-login.php")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        val barUri = buildUri("public-api.wordpress.com", "bar")
        whenever(uri.copy("bar")).thenReturn(barUri)

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `post mbar URL triggers the editor`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(postUrl)
        val expectedAction = NavigateAction.OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(postUrl)).thenReturn(expectedAction)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `does not handle pages url`() {
        val uri = buildUri("wordpress.com", "pages")

        val shouldHandleUri = viewModel.handleUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `does not handle app link to posts`() {
        val uri = buildUri("pages", "")

        val shouldHandleUri = viewModel.handleUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `opens navigate action from editor link handler`() {
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "post", siteUrl)
        whenever(editorLinkHandler.isEditorUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(uri)).thenReturn(expected)

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(expected)
    }

    @Test
    fun `opens navigate action from stats link handler`() {
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "stats", siteUrl)
        whenever(statsLinkHandler.isStatsUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenStats
        whenever(statsLinkHandler.buildOpenStatsNavigateAction(uri)).thenReturn(expected)

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue()
        assertThat(navigateAction).isEqualTo(expected)
    }

    @Test
    fun `view post mbar URL triggers the reader when it can be resolved`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "read")
        var navigateAction: NavigateAction? = null
        val expectedAction = NavigateAction.OpenInReader(redirect)

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        whenever(readerLinkHandler.isReaderUrl(redirect)).thenReturn(true)
        whenever(readerLinkHandler.buildOpenInReaderNavigateAction(redirect)).thenReturn(expectedAction)

        viewModel.navigateAction.observeForever { navigateAction = it?.getContentIfNotHandled() }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue
        assertThat(navigateAction).isEqualTo(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    // TODO Fix this test
    @Ignore("Temporarily ignoring this test as it will be fixed later on")
    @Test
    fun `view post mbar URL triggers the browser when it can't be resolved`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "read")
        val barUri = buildUri("public-api.wordpress.com", "bar")
        var navigateAction: NavigateAction? = null
        val expectedAction = NavigateAction.OpenInBrowser(barUri)

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        whenever(readerLinkHandler.isReaderUrl(redirect)).thenReturn(false)
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.navigateAction.observeForever { navigateAction = it?.getContentIfNotHandled() }

        val urlHandled = viewModel.handleUrl(uri)

        assertThat(urlHandled).isTrue
        assertThat(navigateAction).isEqualTo(expectedAction)
        verifyZeroInteractions(serverTrackingHandler)
    }
}
