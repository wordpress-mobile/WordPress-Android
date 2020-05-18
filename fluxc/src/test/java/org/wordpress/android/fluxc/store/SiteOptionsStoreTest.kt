package org.wordpress.android.fluxc.store

import com.android.volley.VolleyError
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
import org.wordpress.android.fluxc.model.SiteHomepageSettingsMapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.store.SiteOptionsStore.HomepageUpdatedPayload
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class SiteOptionsStoreTest {
    @Mock lateinit var siteHomepageRestClient: SiteHomepageRestClient
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var siteHomepageSettingsMapper: SiteHomepageSettingsMapper
    @Mock lateinit var homepageSettings: SiteHomepageSettings
    @Mock lateinit var updatedHomepageSettings: SiteHomepageSettings
    @Mock lateinit var updatedPayload: HomepageUpdatedPayload
    private lateinit var store: SiteOptionsStore
    private lateinit var wpComSite: SiteModel
    private lateinit var dotOrgSite: SiteModel

    @Before
    fun setUp() {
        store = SiteOptionsStore(
                initCoroutineEngine(),
                siteHomepageRestClient,
                siteXMLRPCClient,
                siteHomepageSettingsMapper
        )
        wpComSite = SiteModel()
        wpComSite.setIsWPCom(true)
        dotOrgSite = SiteModel()
        dotOrgSite.setIsWPCom(false)
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
    fun `on success returns payload from XMLRPC client`() = test {
        initXMLRPCClient()

        val homepageUpdatedPayload = store.updateHomepage(dotOrgSite, homepageSettings)

        assertThat(homepageUpdatedPayload.homepageSettings).isEqualTo(updatedHomepageSettings)
        verify(siteXMLRPCClient).updateSiteHomepage(eq(dotOrgSite), eq(homepageSettings), any(), any())
        verifyZeroInteractions(siteHomepageRestClient)
    }

    @Test
    fun `returns error when mapping fails from XMLRPC client`() = test {
        initXMLRPCClient(mappedHomepageSettings = null)

        val homepageUpdatedPayload = store.updateHomepage(dotOrgSite, homepageSettings)

        assertThat(homepageUpdatedPayload.isError).isTrue()
        assertThat(homepageUpdatedPayload.error).isEqualTo(
                SiteOptionsError(
                        GENERIC_ERROR,
                        "Site contains unexpected showOnFront value: posts"
                )
        )
        verify(siteXMLRPCClient).updateSiteHomepage(eq(dotOrgSite), eq(homepageSettings), any(), any())
        verifyZeroInteractions(siteHomepageRestClient)
    }

    @Test
    fun `on error returns payload from XMLRPC client`() = test {
        val apiErrorMessage = "Request failed"
        initXMLRPCClient(
                error = BaseNetworkError(
                        AUTHORIZATION_REQUIRED,
                        apiErrorMessage,
                        VolleyError("Volley error")
                )
        )

        val homepageUpdatedPayload = store.updateHomepage(dotOrgSite, homepageSettings)

        assertThat(homepageUpdatedPayload.isError).isTrue()
        assertThat(homepageUpdatedPayload.error).isEqualTo(
                SiteOptionsError(
                        SiteOptionsErrorType.AUTHORIZATION_REQUIRED,
                        apiErrorMessage
                )
        )
        verify(siteXMLRPCClient).updateSiteHomepage(eq(dotOrgSite), eq(homepageSettings), any(), any())
        verifyZeroInteractions(siteHomepageRestClient)
    }

    private fun initXMLRPCClient(
        mappedHomepageSettings: SiteHomepageSettings? = updatedHomepageSettings,
        error: BaseNetworkError? = null
    ) {
        val updatedSite = SiteModel()
        whenever(siteHomepageSettingsMapper.map(updatedSite)).thenReturn(mappedHomepageSettings)
        doAnswer {
            if (error != null) {
                val onError = it.getArgument(3) as ((BaseNetworkError) -> Unit)
                onError.invoke(error)
            } else {
                val onSuccess = it.getArgument(2) as ((SiteModel) -> Unit)
                onSuccess.invoke(updatedSite)
            }
        }.whenever(siteXMLRPCClient).updateSiteHomepage(eq(dotOrgSite), eq(homepageSettings), any(), any())
    }
}
