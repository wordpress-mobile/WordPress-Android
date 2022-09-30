package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class HomeLinkHandlerTest {
    @Mock lateinit var site: SiteModel
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
