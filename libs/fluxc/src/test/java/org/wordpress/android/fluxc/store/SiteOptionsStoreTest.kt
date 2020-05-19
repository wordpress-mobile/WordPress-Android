package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.store.SiteOptionsStore.HomepageUpdatedPayload
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class SiteOptionsStoreTest {
    @Mock lateinit var siteHomepageRestClient: SiteHomepageRestClient
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var homepageSettings: SiteHomepageSettings
    @Mock lateinit var updatedPayload: HomepageUpdatedPayload
    private lateinit var store: SiteOptionsStore
    private lateinit var wpComSite: SiteModel
    private lateinit var selfHostedSite: SiteModel

    @Before
    fun setUp() {
        store = SiteOptionsStore(
                initCoroutineEngine(),
                siteHomepageRestClient
        )
        wpComSite = SiteModel()
        wpComSite.setIsWPCom(true)
        selfHostedSite = SiteModel()
        selfHostedSite.setIsWPCom(false)
    }

    @Test
    fun `calls WPCom rest client when site is WPCom`() = test {
        whenever(siteHomepageRestClient.updateHomepage(wpComSite, homepageSettings)).thenReturn(updatedPayload)

        val homepageUpdatedPayload = store.updateHomepage(wpComSite, homepageSettings)

        assertThat(homepageUpdatedPayload).isEqualTo(updatedPayload)
        verify(siteHomepageRestClient).updateHomepage(wpComSite, homepageSettings)
        verifyZeroInteractions(siteXMLRPCClient)
    }

    @Test
    fun `call fails when page for posts and homepage are the same`() = test {
        val invalidHomepageSettings = SiteHomepageSettings.StaticPage(1L, 1L)

        val homepageUpdatedPayload = store.updateHomepage(wpComSite, invalidHomepageSettings)

        assertThat(homepageUpdatedPayload.isError).isTrue()
        assertThat(homepageUpdatedPayload.error).isEqualTo(
                SiteOptionsError(
                        SiteOptionsErrorType.INVALID_PARAMETERS,
                        "Page for posts and page on front cannot be the same"
                )
        )
        verifyZeroInteractions(siteXMLRPCClient)
        verifyZeroInteractions(siteHomepageRestClient)
    }

    @Test
    fun `updates page for posts and keeps page on front when they are different`() = test {
        val updatedPageForPosts: Long = 1
        val currentPageOnFront: Long = 2
        wpComSite.pageOnFront = currentPageOnFront
        doAnswer { HomepageUpdatedPayload(it.getArgument<SiteHomepageSettings>(1)) }.whenever(
                siteHomepageRestClient
        ).updateHomepage(any(), any())
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                updatedPageForPosts, currentPageOnFront
        )

        val homepageUpdatedPayload = store.updatePageForPosts(wpComSite, updatedPageForPosts)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
        verifyZeroInteractions(siteXMLRPCClient)
    }

    @Test
    fun `updates page on front ID to 0 when it is the same as page for posts`() = test {
        val updatedPageForPosts: Long = 1
        val currentPageOnFront: Long = 1
        wpComSite.pageOnFront = currentPageOnFront
        doAnswer { HomepageUpdatedPayload(it.getArgument<SiteHomepageSettings>(1)) }.whenever(
                siteHomepageRestClient
        ).updateHomepage(any(), any())
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                updatedPageForPosts, 0
        )

        val homepageUpdatedPayload = store.updatePageForPosts(wpComSite, updatedPageForPosts)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
        verifyZeroInteractions(siteXMLRPCClient)
    }

    @Test
    fun `updates page for posts ID to 0 when it is the same as page on front`() = test {
        val updatedPageOnFront: Long = 1
        val currentPageForPosts: Long = 1
        wpComSite.pageForPosts = currentPageForPosts
        doAnswer { HomepageUpdatedPayload(it.getArgument<SiteHomepageSettings>(1)) }.whenever(
                siteHomepageRestClient
        ).updateHomepage(any(), any())
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                0, updatedPageOnFront
        )

        val homepageUpdatedPayload = store.updatePageOnFront(wpComSite, updatedPageOnFront)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
        verifyZeroInteractions(siteXMLRPCClient)
    }

    @Test
    fun `updates page on front and keeps page for posts when they are different`() = test {
        val updatedPageOnFront: Long = 1
        val currentPageForPosts: Long = 2
        wpComSite.pageForPosts = currentPageForPosts
        doAnswer { HomepageUpdatedPayload(it.getArgument<SiteHomepageSettings>(1)) }.whenever(
                siteHomepageRestClient
        ).updateHomepage(any(), any())
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                currentPageForPosts, updatedPageOnFront
        )

        val homepageUpdatedPayload = store.updatePageOnFront(wpComSite, updatedPageOnFront)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
        verifyZeroInteractions(siteXMLRPCClient)
    }
}
