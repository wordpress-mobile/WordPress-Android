package org.wordpress.android.ui.deeplinks

import android.net.Uri
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.LoginForResult
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

class DeepLinkingIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock lateinit var editorLinkHandler: EditorLinkHandler
    @Mock lateinit var statsLinkHandler: StatsLinkHandler
    @Mock lateinit var startLinkHandler: StartLinkHandler
    @Mock lateinit var readerLinkHandler: ReaderLinkHandler
    @Mock lateinit var notificationsLinkHandler: NotificationsLinkHandler
    @Mock lateinit var pagesLinkHandler: PagesLinkHandler
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var serverTrackingHandler: ServerTrackingHandler
    @Mock lateinit var deepLinkTrackingHelper: DeepLinkTrackingHelper
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel
    private var isFinished = false
    private lateinit var navigateActions: MutableList<NavigateAction>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = DeepLinkingIntentReceiverViewModel(
                TEST_DISPATCHER,
                editorLinkHandler,
                statsLinkHandler,
                startLinkHandler,
                readerLinkHandler,
                pagesLinkHandler,
                notificationsLinkHandler,
                deepLinkUriUtils,
                accountStore,
                serverTrackingHandler,
                deepLinkTrackingHelper,
                analyticsUtilsWrapper
        )
        isFinished = false
        viewModel.finish.observeForever {
            it?.getContentIfNotHandled()?.let {
                isFinished = true
            }
        }
        navigateActions = mutableListOf()
        viewModel.navigateAction.observeForever {
            it?.getContentIfNotHandled()?.let {
                navigateActions.add(it)
            }
        }
        whenever(accountStore.hasAccessToken()).thenReturn(true)
    }

    @Test
    fun `does not navigate and finishes on WPcom URL`() {
        val uri = buildUri("wordpress.com")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `does not navigate and finishes on non-mobile URL`() {
        val uri = buildUri("public-api.wordpress.com")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `mbar URL without redirect parameter replaced mbar to bar and opened in browser`() {
        val uri = initTrackingUri()
        val barUri = buildUri("public-api.wordpress.com")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri)

        assertUriHandled(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `create site mbar URL triggers the Site Creation flow`() {
        val startUrl = initStartUrl()
        val wpLoginUri = initWpLoginUri(startUrl)
        val uri = initTrackingUri(wpLoginUri)

        whenever(startLinkHandler.buildNavigateAction(startUrl)).thenReturn(StartCreateSiteFlow)

        viewModel.start(null, uri)

        assertUriHandled(StartCreateSiteFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `wp-login mbar URL redirects user to browser with missing second redirect`() {
        val wpLoginUri = initWpLoginUri()
        val uri = initTrackingUri(wpLoginUri)
        val barUri = buildUri("public-api.wordpress.com")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri)

        assertUriHandled(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `post mbar URL triggers the editor`() {
        val postUrl = initPostUrl()
        val uri = initTrackingUri(postUrl)
        val expectedAction = OpenEditor
        whenever(editorLinkHandler.buildNavigateAction(postUrl)).thenReturn(expectedAction)

        viewModel.start(null, uri)

        assertUriHandled(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `does not handle pages url`() {
        val uri = buildUri("wordpress.com")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `does not handle app link to pages`() {
        val uri = buildUri("pages")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `opens navigate action from editor link handler`() {
        val (uri, expected) = initEditorLinkHandler()

        viewModel.start(null, uri)

        assertUriHandled(expected)
    }

    @Test
    fun `opens login when user logged out`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        val (uri, _) = initEditorLinkHandler()

        viewModel.start(null, uri)

        assertUriHandled(LoginForResult)
    }

    @Test
    fun `opens navigate action from stats link handler`() {
        val uri = buildUri("wordpress.com")
        whenever(statsLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenStats
        whenever(statsLinkHandler.buildNavigateAction(uri)).thenReturn(expected)

        viewModel.start(null, uri)

        assertUriHandled(expected)
    }

    @Test
    fun `opens navigate action from notifications link handler`() {
        val uri = buildUri("wordpress.com")
        whenever(notificationsLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenNotifications
        whenever(notificationsLinkHandler.buildNavigateAction(uri)).thenReturn(expected)

        viewModel.start(null, uri)

        assertUriHandled(expected)
    }

    @Test
    fun `view post mbar URL triggers the reader when it can be resolved`() {
        val redirect = buildUri("wordpress.com")
        val uri = initTrackingUri(redirect)
        val expectedAction = NavigateAction.OpenInReader(redirect)

        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        whenever(readerLinkHandler.shouldHandleUrl(redirect)).thenReturn(true)
        whenever(readerLinkHandler.buildNavigateAction(redirect)).thenReturn(expectedAction)

        viewModel.start(null, uri)

        assertUriHandled(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `view post mbar URL triggers the browser when it can't be resolved`() {
        val redirect = buildUri("wordpress.com")
        val uri = initTrackingUri(redirect)
        val barUri = buildUri("public-api.wordpress.com")
        val expectedAction = NavigateAction.OpenInBrowser(barUri)

        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        whenever(readerLinkHandler.shouldHandleUrl(redirect)).thenReturn(false)
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri)

        assertUriHandled(expectedAction)
        verifyZeroInteractions(serverTrackingHandler)
    }

    @Test
    fun `tracks deeplink when action not null and URL null`() {
        val action = "VIEW"

        viewModel.start(action, null)

        verify(analyticsUtilsWrapper).trackWithDeepLinkData(eq(DEEP_LINKED), eq(action), eq(""), isNull())
    }

    @Test
    fun `tracks deeplink when action not null and URL not null`() {
        val action = "VIEW"
        val host = "wordpress.com"
        val uriWrapper = buildUri(host)
        val mockedUri = mock<Uri>()
        whenever(uriWrapper.uri).thenReturn(mockedUri)

        viewModel.start(action, uriWrapper)

        verify(analyticsUtilsWrapper).trackWithDeepLinkData(DEEP_LINKED, action, host, mockedUri)
    }

    @Test
    fun `on successful login rehandles the cached URL`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false, true)
        val (uri, expected) = initEditorLinkHandler()
        viewModel.start(null, uri)

        viewModel.onSuccessfulLogin()

        assertThat(navigateActions).containsExactly(LoginForResult, expected)
    }

    private fun assertUriNotHandled() {
        assertThat(isFinished).isTrue()
        assertThat(navigateActions).isEmpty()
    }

    private fun assertUriHandled(navigateAction: NavigateAction) {
        assertThat(isFinished).isFalse()
        assertThat(navigateActions.last()).isEqualTo(navigateAction)
    }

    private fun initEditorLinkHandler(): Pair<UriWrapper, OpenEditor> {
        val uri = buildUri("wordpress.com")
        whenever(editorLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        val expected = OpenEditor
        whenever(editorLinkHandler.buildNavigateAction(uri)).thenReturn(expected)
        return Pair(uri, expected)
    }

    private fun initTrackingUri(redirectTo: UriWrapper? = null): UriWrapper {
        val uri = initRedirectUri("public-api.wordpress.com", redirectTo)
        whenever(deepLinkUriUtils.isTrackingUrl(uri)).thenReturn(true)
        return uri
    }

    private fun initWpLoginUri(redirectTo: UriWrapper? = null): UriWrapper {
        val uri = initRedirectUri("wordpress.com", redirectTo)
        whenever(deepLinkUriUtils.isWpLoginUrl(uri)).thenReturn(true)
        return uri
    }

    private fun initRedirectUri(host: String, redirectTo: UriWrapper? = null): UriWrapper {
        val uri = buildUri(host)
        redirectTo?.let {
            whenever(deepLinkUriUtils.getRedirectUri(uri)).thenReturn(it)
        }
        return uri
    }

    private fun initStartUrl(): UriWrapper {
        val uri = mock<UriWrapper>()
        whenever(startLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        return uri
    }

    private fun initPostUrl(): UriWrapper {
        val uri = mock<UriWrapper>()
        whenever(editorLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        return uri
    }
}
