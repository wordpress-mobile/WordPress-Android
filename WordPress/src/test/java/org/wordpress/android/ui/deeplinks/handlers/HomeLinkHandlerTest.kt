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
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenLoginPrologue
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenMySite
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class HomeLinkHandlerTest {
    @Mock lateinit var accountStore: AccountStore
    private lateinit var homeLinkHandler: HomeLinkHandler

    @Before
    fun setUp() {
        homeLinkHandler = HomeLinkHandler(accountStore)
    }

    @Test
    fun `returns login prologue action when user is logged out`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        val navigateAction = homeLinkHandler.buildNavigateAction(mock())

        assertThat(navigateAction).isEqualTo(OpenLoginPrologue)
    }

    @Test
    fun `returns my site action when user is logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        val navigateAction = homeLinkHandler.buildNavigateAction(mock())

        assertThat(navigateAction).isEqualTo(OpenMySite)
    }

    @Test
    fun `builds URL for tracking`() {
        val homeUri = buildUri("home")

        val strippedUrl = homeLinkHandler.stripUrl(homeUri)

        assertThat(strippedUrl).isEqualTo("wordpress://home")
    }
}
