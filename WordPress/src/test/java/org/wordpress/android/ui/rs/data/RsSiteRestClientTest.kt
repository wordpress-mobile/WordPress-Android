package org.wordpress.android.ui.rs.data

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.TermsRequestListWithViewContextResponse
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.WpNetworkHeaderMap

/**
 * Covers the term-name cache, which is what the editor's tag list is built from.
 */
class RsSiteRestClientTest {
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val restClient = RsSiteRestClient(mock(), wpApiClientProvider)

    private val siteA = SiteModel().apply { id = 1 }
    private val siteB = SiteModel().apply { id = 2 }

    /**
     * Term ids are per site - on a self-hosted site they start at 1 - so a cache shared across
     * sites would answer with the wrong name, and the editor would save that name over the real
     * tag. The names are display text for ids the post carries, so there is no id to correct it by.
     */
    @Test
    fun `a term name cached for one site is not reused for another`() = runTest {
        val clientA = clientReturning(term(5L, "news"))
        val clientB = clientReturning(term(5L, "recipes"))
        whenever(wpApiClientProvider.getWpApiClient(eq(siteA), anyOrNull())).thenReturn(clientA)
        whenever(wpApiClientProvider.getWpApiClient(eq(siteB), anyOrNull())).thenReturn(clientB)

        val first = restClient.fetchTermNames(siteA, listOf(5L), TermEndpointType.Tags)
        val second = restClient.fetchTermNames(siteB, listOf(5L), TermEndpointType.Tags)

        assertThat(first).isEqualTo(mapOf(5L to "news"))
        assertThat(second).isEqualTo(mapOf(5L to "recipes"))
    }

    @Test
    fun `an id already cached for the same site is not requested again`() = runTest {
        val client = clientReturning(term(5L, "news"))
        whenever(wpApiClientProvider.getWpApiClient(eq(siteA), anyOrNull())).thenReturn(client)

        restClient.fetchTermNames(siteA, listOf(5L), TermEndpointType.Tags)
        val cached = restClient.fetchTermNames(siteA, listOf(5L), TermEndpointType.Tags)

        assertThat(cached).isEqualTo(mapOf(5L to "news"))
        verify(client).request<TermsRequestListWithViewContextResponse>(any())
    }

    /**
     * A partial map is indistinguishable from "the site no longer has those terms", so a caller
     * that stored it would drop the tags that are missing from it on the next save.
     */
    @Test
    fun `a failed lookup reports failure rather than the names it did read`() = runTest {
        val client: WpApiClient = mock()
        whenever(wpApiClientProvider.getWpApiClient(eq(siteA), anyOrNull())).thenReturn(client)
        whenever(client.request<TermsRequestListWithViewContextResponse>(any())).thenReturn(
            WpRequestResult.WpError(
                errorCode = WpErrorCode.CustomException("http_request_failed"),
                errorMessage = "cURL error 28: Operation timed out",
                statusCode = 500u,
                response = "",
                requestUrl = "",
                requestMethod = RequestMethod.GET
            )
        )

        assertThat(restClient.fetchTermNames(siteA, listOf(5L), TermEndpointType.Tags)).isNull()
    }

    @Test
    fun `no ids asks the site for nothing`() = runTest {
        assertThat(restClient.fetchTermNames(siteA, emptyList(), TermEndpointType.Tags)).isEmpty()
        verify(wpApiClientProvider, never()).getWpApiClient(any(), anyOrNull())
    }

    private suspend fun clientReturning(vararg terms: AnyTermWithViewContext): WpApiClient {
        val client: WpApiClient = mock()
        whenever(client.request<TermsRequestListWithViewContextResponse>(any())).thenReturn(
            WpRequestResult.Success(
                TermsRequestListWithViewContextResponse(
                    terms.toList(),
                    mock<WpNetworkHeaderMap>(),
                    null as TermListParams?,
                    null
                )
            )
        )
        return client
    }

    private fun term(id: Long, name: String) = AnyTermWithViewContext(
        id = id,
        count = 0L,
        description = "",
        link = "https://example.com/tag/$name",
        name = name,
        slug = name,
        taxonomy = TaxonomyType.PostTag,
        parent = 0L
    )
}
