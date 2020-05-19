package org.wordpress.android.fluxc.store

import com.android.volley.VolleyError
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteHomepageSettingsMapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class SiteOptionsStoreTest {
    @Mock lateinit var siteHomepageRestClient: SiteHomepageRestClient
    @Mock lateinit var siteHomepageSettingsMapper: SiteHomepageSettingsMapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var homepageSettings: SiteHomepageSettings
    @Mock lateinit var successResponse: Response.Success<UpdateHomepageResponse>
    @Mock lateinit var errorResponse: Response.Error<UpdateHomepageResponse>
    @Mock lateinit var responseData: UpdateHomepageResponse
    private lateinit var store: SiteOptionsStore
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>
    private lateinit var wpComSite: SiteModel
    private lateinit var selfHostedSite: SiteModel

    @Before
    fun setUp() {
        store = SiteOptionsStore(
                initCoroutineEngine(),
                dispatcher,
                siteHomepageSettingsMapper,
                siteHomepageRestClient
        )
        wpComSite = SiteModel()
        wpComSite.setIsWPCom(true)
        selfHostedSite = SiteModel()
        selfHostedSite.setIsWPCom(false)
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `calls rest client, updates site and returns success response`() = test {
        val pageForPostsId: Long = 1
        val pageOnFrontId: Long = 2
        val updatedSettings = SiteHomepageSettings.StaticPage(pageForPostsId, pageOnFrontId)
        initSuccessResponse(updatedSettings)

        val updatedPayload = store.updateHomepage(wpComSite, homepageSettings)

        assertThat(updatedPayload.homepageSettings).isEqualTo(updatedSettings)
        verify(siteHomepageRestClient).updateHomepage(wpComSite, homepageSettings)
        assertThat(wpComSite.showOnFront).isEqualTo(ShowOnFront.PAGE.value)
        assertThat(wpComSite.pageOnFront).isEqualTo(pageOnFrontId)
        assertThat(wpComSite.pageForPosts).isEqualTo(pageForPostsId)

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.lastValue.type).isEqualTo(SiteAction.UPDATE_SITE)
        assertThat(actionCaptor.lastValue.payload).isEqualTo(wpComSite)
    }

    @Test
    fun `calls rest client and returns error response`() = test {
        val errorMessage = "Message"
        initErrorResponse(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR, errorMessage, VolleyError(errorMessage))))

        val updatedPayload = store.updateHomepage(wpComSite, homepageSettings)

        assertThat(updatedPayload.isError).isTrue()
        assertThat(updatedPayload.error.type).isEqualTo(SiteOptionsErrorType.API_ERROR)
        assertThat(updatedPayload.error.message).isEqualTo(errorMessage)
        verify(siteHomepageRestClient).updateHomepage(wpComSite, homepageSettings)
        verifyZeroInteractions(dispatcher)
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
        verifyZeroInteractions(siteHomepageRestClient)
    }

    @Test
    fun `updates page for posts and keeps page on front when they are different`() = test {
        val updatedPageForPosts: Long = 1
        val currentPageOnFront: Long = 2
        wpComSite.pageOnFront = currentPageOnFront
        initSuccessResponse(SiteHomepageSettings.StaticPage(updatedPageForPosts, currentPageOnFront))
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                updatedPageForPosts, currentPageOnFront
        )

        val homepageUpdatedPayload = store.updatePageForPosts(wpComSite, updatedPageForPosts)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
    }

    @Test
    fun `updates page on front ID to 0 when it is the same as page for posts`() = test {
        val updatedPageForPosts: Long = 1
        val currentPageOnFront: Long = 1
        wpComSite.pageOnFront = currentPageOnFront
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                updatedPageForPosts, 0
        )
        initSuccessResponse(expectedHomepageSettings)

        val homepageUpdatedPayload = store.updatePageForPosts(wpComSite, updatedPageForPosts)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
    }

    @Test
    fun `updates page for posts ID to 0 when it is the same as page on front`() = test {
        val updatedPageOnFront: Long = 1
        val currentPageForPosts: Long = 1
        wpComSite.pageForPosts = currentPageForPosts
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                0, updatedPageOnFront
        )
        initSuccessResponse(expectedHomepageSettings)

        val homepageUpdatedPayload = store.updatePageOnFront(wpComSite, updatedPageOnFront)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
    }

    @Test
    fun `updates page on front and keeps page for posts when they are different`() = test {
        val updatedPageOnFront: Long = 1
        val currentPageForPosts: Long = 2
        wpComSite.pageForPosts = currentPageForPosts
        val expectedHomepageSettings = SiteHomepageSettings.StaticPage(
                currentPageForPosts, updatedPageOnFront
        )
        initSuccessResponse(expectedHomepageSettings)

        val homepageUpdatedPayload = store.updatePageOnFront(wpComSite, updatedPageOnFront)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(
                expectedHomepageSettings
        )
        verify(siteHomepageRestClient).updateHomepage(eq(wpComSite), eq(expectedHomepageSettings))
    }

    private suspend fun initSuccessResponse(
        updatedSettings: SiteHomepageSettings
    ) {
        whenever(siteHomepageRestClient.updateHomepage(eq(wpComSite), any())).thenReturn(successResponse)
        whenever(successResponse.data).thenReturn(responseData)
        whenever(siteHomepageSettingsMapper.map(responseData)).thenReturn(updatedSettings)
    }

    private suspend fun initErrorResponse(
        error: WPComGsonNetworkError? = null
    ) {
        whenever(siteHomepageRestClient.updateHomepage(eq(wpComSite), any())).thenReturn(errorResponse)
        whenever(errorResponse.error).thenReturn(error)
    }
}
