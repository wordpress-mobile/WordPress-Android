package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class StartLinkHandlerTest {
    @Mock
    lateinit var accountStore: AccountStore
    private lateinit var startLinkHandler: StartLinkHandler

    @Before
    fun setUp() {
        startLinkHandler = StartLinkHandler(accountStore)
    }

    @Test
    fun `handles start URI is true`() {
        val startUri = buildUri("wordpress.com", "start")

        val isStartUri = startLinkHandler.shouldHandleUrl(startUri)

        assertThat(isStartUri).isTrue()
    }

    @Test
    fun `does not handle start URI with different host`() {
        val startUri = buildUri("wordpress.org", "start")

        val isStartUri = startLinkHandler.shouldHandleUrl(startUri)

        assertThat(isStartUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val startUri = buildUri("wordpress.com", "stop")

        val isStartUri = startLinkHandler.shouldHandleUrl(startUri)

        assertThat(isStartUri).isFalse()
    }

    @Test
    fun `returns site creation flow action when user logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        val navigateAction = startLinkHandler.buildNavigateAction(mock())

        assertThat(navigateAction).isEqualTo(StartCreateSiteFlow)
    }

    @Test
    fun `returns sign in action when user not logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        val navigateAction = startLinkHandler.buildNavigateAction(mock())

        assertThat(navigateAction).isEqualTo(ShowSignInFlow)
    }

    @Test
    fun `builds URL for tracking`() {
        val startUri = buildUri("wordpress.com", "start")

        val strippedUrl = startLinkHandler.stripUrl(startUri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/start")
    }
}
