package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator
import org.wordpress.android.ui.deeplinks.buildUri
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class DomainManagementLinkHandlerTest {
    private lateinit var handler: DomainManagementLinkHandler

    @Before
    fun setUp() {
        handler = DomainManagementLinkHandler()
    }

    @Test
    fun `WHEN the me domains scheme is passed THEN the url is handled`() {
        val uri = buildUri("wordpress.com", "me", "domains")

        val handled = handler.shouldHandleUrl(uri)

        assertThat(handled).isTrue()
    }

    @Test
    fun `WHEN the domains manage scheme is passed THEN the url is handled`() {
        val uri = buildUri("wordpress.com", "domains", "manage")

        val handled = handler.shouldHandleUrl(uri)

        assertThat(handled).isTrue()
    }

    @Test
    fun `WHEN a different host is passed THEN the url is not handled`() {
        val uri = buildUri("wordpress.org", "domains", "manage")

        val handled = handler.shouldHandleUrl(uri)

        assertThat(handled).isFalse()
    }

    @Test
    fun `WHEN a different path is passed THEN the url is not handled`() {
        val uri = buildUri("wordpress.com", "different")

        val handled = handler.shouldHandleUrl(uri)

        assertThat(handled).isFalse()
    }

    @Test
    fun `WHEN the navigation action is requested THEN the DomainManagement is returned`() {
        val navigateAction = handler.buildNavigateAction(mock())

        assertEquals(navigateAction, DeepLinkNavigator.NavigateAction.DomainManagement)
    }

    @Test
    fun `WHEN the domains manage scheme is used THEN the correct url for tracking is returned`() {
        val uri = buildUri("wordpress.com", "domains", "manage")

        val strippedUrl = handler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/domains/manage")
    }

    @Test
    fun `WHEN the me domains scheme is used THEN the correct url for tracking is returned`() {
        val uri = buildUri("wordpress.com", "me", "domains")

        val strippedUrl = handler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/me/domains")
    }
}
