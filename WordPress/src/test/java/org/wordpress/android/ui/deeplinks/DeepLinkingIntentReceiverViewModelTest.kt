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
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel
    private val startUrl = buildUri("wordpress.com", "start")
    private val postUrl = buildUri("wordpress.com", "post")
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
                analyticsUtilsWrapper
        )
        whenever(startLinkHandler.isStartUrl(startUrl)).thenReturn(true)
        whenever(editorLinkHandler.isEditorUrl(postUrl)).thenReturn(true)
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
        val uri = buildUri("wordpress.com", "bar")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `does not navigate and finishes on non-mobile URL`() {
        val uri = buildUri("public-api.wordpress.com", "bar")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `mbar URL without redirect parameter replaced mbar to bar and opened in browser`() {
        val uri = buildUri("public-api.wordpress.com", "mbar")
        val barUri = buildUri("public-api.wordpress.com", "bar")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri)

        assertUriHandled(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `create site mbar URL triggers the Site Creation flow`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val firstRedirect = buildUri("wordpress.com", "wp-login.php", "redirect_to...")

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getUriFromQueryParameter(firstRedirect, "redirect_to")).thenReturn(startUrl)
        whenever(startLinkHandler.buildNavigateAction()).thenReturn(StartCreateSiteFlow)

        viewModel.start(null, uri)

        assertUriHandled(StartCreateSiteFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `wp-login mbar URL redirects user to browser with missing second redirect`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "wp-login.php")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        val barUri = buildUri("public-api.wordpress.com", "bar")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri)

        assertUriHandled(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `post mbar URL triggers the editor`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(postUrl)
        val expectedAction = NavigateAction.OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(postUrl)).thenReturn(expectedAction)

        viewModel.start(null, uri)

        assertUriHandled(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `does not handle pages url`() {
        val uri = buildUri("wordpress.com", "pages")

        viewModel.start(null, uri)

        assertUriNotHandled()
    }

    @Test
    fun `does not handle app link to pages`() {
        val uri = buildUri("pages", "")

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
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "stats", siteUrl)
        whenever(statsLinkHandler.isStatsUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenStats
        whenever(statsLinkHandler.buildOpenStatsNavigateAction(uri)).thenReturn(expected)

        viewModel.start(null, uri)

        assertUriHandled(expected)
    }

    @Test
    fun `opens navigate action from notifications link handler`() {
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "notifications", siteUrl)
        whenever(notificationsLinkHandler.isNotificationsUrl(uri)).thenReturn(true)
        val expected = NavigateAction.OpenNotifications
        whenever(notificationsLinkHandler.buildNavigateAction()).thenReturn(expected)

        viewModel.start(null, uri)

        assertUriHandled(expected)
    }

    @Test
    fun `view post mbar URL triggers the reader when it can be resolved`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "read")
        val expectedAction = NavigateAction.OpenInReader(redirect)

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        whenever(readerLinkHandler.isReaderUrl(redirect)).thenReturn(true)
        whenever(readerLinkHandler.buildOpenInReaderNavigateAction(redirect)).thenReturn(expectedAction)

        viewModel.start(null, uri)

        assertUriHandled(expectedAction)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `view post mbar URL triggers the browser when it can't be resolved`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "read")
        val barUri = buildUri("public-api.wordpress.com", "bar")
        val expectedAction = NavigateAction.OpenInBrowser(barUri)

        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        whenever(readerLinkHandler.isReaderUrl(redirect)).thenReturn(false)
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
        val uriWrapper = buildUri(host, "read")
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
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "post", siteUrl)
        whenever(editorLinkHandler.isEditorUrl(uri)).thenReturn(true)
        val expected = OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(uri)).thenReturn(expected)
        return Pair(uri, expected)
    }
}
