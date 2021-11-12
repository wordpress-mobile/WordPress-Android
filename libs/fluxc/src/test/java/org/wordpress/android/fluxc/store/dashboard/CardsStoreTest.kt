package org.wordpress.android.fluxc.store.dashboard

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardsMapper
import org.wordpress.android.fluxc.model.dashboard.CardsModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.FetchedCardsPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
val POST_DATE = Date()
const val POST_FEATURED_IMAGE = "featuredImage"

/* RESPONSE */

private val POST_RESPONSE = PostResponse(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        date = POST_DATE,
        featuredImage = POST_FEATURED_IMAGE
)

private val POSTS_RESPONSE = PostsResponse(
        hasPublished = false,
        draft = listOf(POST_RESPONSE),
        scheduled = listOf(POST_RESPONSE)
)

private val CARDS_RESPONSE = CardsResponse(
        posts = POSTS_RESPONSE
)

/* MODEL */

private val POST_MODEL = CardsModel.PostsModel.PostModel(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        date = POST_DATE,
        featuredImage = POST_FEATURED_IMAGE
)

private val POSTS_MODEL = CardsModel.PostsModel(
        hasPublished = false,
        draft = listOf(POST_MODEL),
        scheduled = listOf(POST_MODEL)
)

private val CARDS_MODEL = CardsModel(
        posts = POSTS_MODEL
)

@RunWith(MockitoJUnitRunner::class)
class CardsStoreTest {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: CardsRestClient

    private lateinit var cardsStore: CardsStore

    @Before
    fun setUp() {
        cardsStore = CardsStore(
                restClient,
                CardsMapper(),
                initCoroutineEngine()
        )
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then cards model is returned`() = test {
        val payload = FetchedCardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isEqualTo(CARDS_MODEL)
    }

    @Test
    fun `given cards error, when fetch cards gets triggered, then cards error is returned`() = test {
        val errorType = CardsErrorType.API_ERROR
        val payload = FetchedCardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }
}
