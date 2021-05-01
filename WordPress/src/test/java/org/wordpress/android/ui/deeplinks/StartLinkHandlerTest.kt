package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow

@RunWith(MockitoJUnitRunner::class)
class StartLinkHandlerTest {
    @Mock lateinit var accountStore: AccountStore
    private lateinit var startLinkHandler: StartLinkHandler

    @Before
    fun setUp() {
        startLinkHandler = StartLinkHandler(accountStore)
    }

    @Test
    fun `handles start URI is true`() {
        val startUri = buildUri("wordpress.com", "start")

        val isStartUri = startLinkHandler.isStartUrl(startUri)

        assertThat(isStartUri).isTrue()
    }

    @Test
    fun `does not handle start URI with different host`() {
        val startUri = buildUri("wordpress.org", "start")

        val isStartUri = startLinkHandler.isStartUrl(startUri)

        assertThat(isStartUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val startUri = buildUri("wordpress.com", "stop")

        val isStartUri = startLinkHandler.isStartUrl(startUri)

        assertThat(isStartUri).isFalse()
    }

    @Test
    fun `returns site creation flow action when user logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        val navigateAction = startLinkHandler.buildNavigateAction()

        assertThat(navigateAction).isEqualTo(StartCreateSiteFlow)
    }

    @Test
    fun `returns sign in action when user not logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        val navigateAction = startLinkHandler.buildNavigateAction()

        assertThat(navigateAction).isEqualTo(ShowSignInFlow)
    }
}
