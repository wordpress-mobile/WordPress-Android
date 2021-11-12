package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.store.dashboard.CardsStore.FetchedCardsPayload
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class CardsRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var restClient: CardsRestClient

    @Before
    fun setUp() {
        restClient = CardsRestClient(
                dispatcher,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `skeleton test`() = test {
        val result = restClient.fetchCards(site)

        assertThat(result).isEqualTo(
                FetchedCardsPayload(
                        CardsResponse(
                                posts = PostsResponse(
                                        hasPublished = false,
                                        draft = listOf(),
                                        scheduled = listOf()
                                )
                        )
                )
        )
    }
}
