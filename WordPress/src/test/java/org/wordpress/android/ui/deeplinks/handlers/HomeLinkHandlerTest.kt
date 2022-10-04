package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.buildUri

class HomeLinkHandlerTest {
    private lateinit var homeLinkHandler: HomeLinkHandler

    @Before
    fun setUp() {
        homeLinkHandler = HomeLinkHandler()
    }

    @Test
    fun `opens home screen from app link`() {
        val uri = buildUri(host = "home")

        val navigateAction = homeLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenHome)
    }
}
