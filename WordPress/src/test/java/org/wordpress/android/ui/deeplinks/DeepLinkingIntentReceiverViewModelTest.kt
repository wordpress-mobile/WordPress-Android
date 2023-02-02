package org.wordpress.android.ui.deeplinks

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkEntryPoint.DEFAULT
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.deeplinks.handlers.ServerTrackingHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
class DeepLinkingIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var deepLinkHandlers: DeepLinkHandlers

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils

    @Mock
    lateinit var serverTrackingHandler: ServerTrackingHandler

    @Mock
    lateinit var deepLinkTrackingUtils: DeepLinkTrackingUtils

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    @Mock
    lateinit var contextProvider: ContextProvider

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel
    private var isFinished = false
    private lateinit var navigateActions: MutableList<NavigateAction>

    @Before
    fun setUp() {
        viewModel = DeepLinkingIntentReceiverViewModel(
            testDispatcher(),
            deepLinkHandlers,
            deepLinkUriUtils,
            accountStore,
            serverTrackingHandler,
            deepLinkTrackingUtils,
            analyticsUtilsWrapper,
            openWebLinksWithJetpackHelper
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

        viewModel.start(null, uri, DEFAULT, null)

        assertUriNotHandled()
    }

    @Test
    fun `does not navigate and finishes on non-mobile URL`() {
        val uri = buildUri("public-api.wordpress.com")

        viewModel.start(null, uri, DEFAULT, null)

        assertUriNotHandled()
    }

    @Test
    fun `mbar URL without redirect parameter replaced mbar to bar and opened in browser`() {
        val uri = initTrackingUri()
        val barUri = buildUri("public-api.wordpress.com")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri, DEFAULT, null)

        assertUriHandled(OpenInBrowser(barUri))
    }

    @Test
    fun `URL passed to deep link handler from redirect parameter`() {
        val startUrl = mock<UriWrapper>()
        val wpLoginUri = initWpLoginUri(startUrl)
        val uri = initTrackingUri(wpLoginUri)

        whenever(deepLinkHandlers.buildNavigateAction(startUrl)).thenReturn(StartCreateSiteFlow)

        viewModel.start(null, uri, DEFAULT, null)

        assertUriHandled(StartCreateSiteFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `URL opened in browser from redirect parameter when deep link handler cannot handle it`() {
        val startUrl = mock<UriWrapper>()
        val wpLoginUri = initWpLoginUri(startUrl)
        val uri = initTrackingUri(wpLoginUri)
        val barUri = buildUri("public-api.wordpress.com")

        whenever(deepLinkHandlers.buildNavigateAction(startUrl)).thenReturn(null)
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri, DEFAULT, null)

        assertUriHandled(OpenInBrowser(barUri))
    }

    @Test
    fun `wp-login mbar URL redirects user to browser with missing second redirect`() {
        val wpLoginUri = initWpLoginUri()
        val uri = initTrackingUri(wpLoginUri)
        val barUri = buildUri("public-api.wordpress.com")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.start(null, uri, DEFAULT, null)

        assertUriHandled(OpenInBrowser(barUri))
    }

    @Test
    fun `tracks deeplink when action not null and URL null`() {
        val action = "VIEW"

        viewModel.start(action, null, DEFAULT, null)

        verify(analyticsUtilsWrapper).trackWithDeepLinkData(eq(DEEP_LINKED), eq(action), eq(""), isNull())
    }

    @Test
    fun `tracks deeplink when action not null and URL not null`() {
        val action = "VIEW"
        val host = "wordpress.com"
        val uriWrapper = buildUri(host)
        val mockedUri = mock<Uri>()
        whenever(uriWrapper.uri).thenReturn(mockedUri)

        viewModel.start(action, uriWrapper, DEFAULT, null)

        verify(analyticsUtilsWrapper).trackWithDeepLinkData(DEEP_LINKED, action, host, mockedUri)
    }

    private fun assertUriNotHandled() {
        assertThat(isFinished).isTrue
        assertThat(navigateActions).isEmpty()
    }

    private fun assertUriHandled(navigateAction: NavigateAction) {
        assertThat(isFinished).isFalse
        assertThat(navigateActions.last()).isEqualTo(navigateAction)
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
}
