package org.wordpress.android.fluxc.store.dashboard

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.TodaysStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao.CardEntity
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsPayload
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull

/* SITE */

const val SITE_LOCAL_ID = 1

/* TODAY'S STATS */

const val TODAYS_STATS_VIEWS = 100
const val TODAYS_STATS_VISITORS = 30
const val TODAYS_STATS_LIKES = 50
const val TODAYS_STATS_COMMENTS = 10

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
const val POST_FEATURED_IMAGE = "featuredImage"
const val POST_DATE = "2021-12-27 11:33:55"

/* CARD TYPES */

private val CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)

/* RESPONSE */

private val TODAYS_STATS_RESPONSE = TodaysStatsResponse(
        views = TODAYS_STATS_VIEWS,
        visitors = TODAYS_STATS_VISITORS,
        likes = TODAYS_STATS_LIKES,
        comments = TODAYS_STATS_COMMENTS
)

private val POST_RESPONSE = PostResponse(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        featuredImage = POST_FEATURED_IMAGE,
        date = POST_DATE
)

private val POSTS_RESPONSE = PostsResponse(
        hasPublished = false,
        draft = listOf(POST_RESPONSE),
        scheduled = listOf(POST_RESPONSE)
)

private val CARDS_RESPONSE = CardsResponse(
        todaysStats = TODAYS_STATS_RESPONSE,
        posts = POSTS_RESPONSE
)

/* MODEL */
private val TODAYS_STATS_MODEL = TodaysStatsCardModel(
        views = TODAYS_STATS_VIEWS,
        visitors = TODAYS_STATS_VISITORS,
        likes = TODAYS_STATS_LIKES,
        comments = TODAYS_STATS_COMMENTS
)

private val POST_MODEL = PostCardModel(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        featuredImage = POST_FEATURED_IMAGE,
        date = CardsUtils.fromDate(POST_DATE)
)

private val POSTS_MODEL = PostsCardModel(
        hasPublished = false,
        draft = listOf(POST_MODEL),
        scheduled = listOf(POST_MODEL)
)

private val CARDS_MODEL = listOf(
        TODAYS_STATS_MODEL,
        POSTS_MODEL
)

/* ENTITY */
private val TODAYS_STATS_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.TODAYS_STATS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(TODAYS_STATS_MODEL)
)

private val POSTS_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.POSTS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(POSTS_MODEL)
)

private val CARDS_ENTITY = listOf(
        TODAYS_STATS_ENTITY,
        POSTS_ENTITY
)

@RunWith(MockitoJUnitRunner::class)
class CardsStoreTest {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: CardsRestClient
    @Mock private lateinit var dao: CardsDao

    private lateinit var cardsStore: CardsStore

    @Before
    fun setUp() {
        cardsStore = CardsStore(
                restClient,
                dao,
                initCoroutineEngine()
        )
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
    }

    @Test
    fun `given all card types, when fetch cards triggered, then all cards model is inserted into db`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        cardsStore.fetchCards(siteModel, CARD_TYPES)

        verify(dao).insertWithDate(siteModel.id, CARDS_MODEL)
    }

    @Test
    fun `given todays stats type, when fetch cards triggered, then today's stats card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(todaysStats = TODAYS_STATS_RESPONSE))
        whenever(restClient.fetchCards(siteModel, listOf(CardModel.Type.TODAYS_STATS))).thenReturn(payload)

        cardsStore.fetchCards(siteModel, listOf(CardModel.Type.TODAYS_STATS))

        verify(dao).insertWithDate(siteModel.id, listOf(TODAYS_STATS_MODEL))
    }

    @Test
    fun `given posts type, when fetch cards triggered, then post card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(posts = POSTS_RESPONSE))
        whenever(restClient.fetchCards(siteModel, listOf(CardModel.Type.POSTS))).thenReturn(payload)

        cardsStore.fetchCards(siteModel, listOf(CardModel.Type.POSTS))

        verify(dao).insertWithDate(siteModel.id, listOf(POSTS_MODEL))
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then empty cards model is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel, CARD_TYPES)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given card response with exception, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)
        whenever(dao.insertWithDate(siteModel.id, CARDS_MODEL)).thenThrow(IllegalStateException("Error"))

        val result = cardsStore.fetchCards(siteModel, CARD_TYPES)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.GENERIC_ERROR, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given cards error, when fetch cards gets triggered, then cards error is returned`() = test {
        val errorType = CardsErrorType.API_ERROR
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel, CARD_TYPES)

        assertThat(result.model).isNull()
        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given authorization required, when fetch cards gets triggered, then db is cleared of cards model`() = test {
        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        cardsStore.fetchCards(siteModel, CARD_TYPES)

        verify(dao).clear()
    }

    @Test
    fun `given authorization required, when fetch cards gets triggered, then empty cards model is returned`() = test {
        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel, CARD_TYPES)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given empty cards payload, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload<CardsResponse>()
        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel, CARD_TYPES)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.INVALID_RESPONSE, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `when get cards gets triggered, then a flow of cards model is returned`() = test {
        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES)).thenReturn(flowOf(CARDS_ENTITY))

        val result = cardsStore.getCards(siteModel, CARD_TYPES).single()

        assertThat(result).isEqualTo(CardsResult(CARDS_MODEL))
    }

    @Test
    fun `when get cards gets triggered for today's stats only, then a flow of today's stats card model is returned`() =
            test {
                whenever(dao.get(SITE_LOCAL_ID, listOf(CardModel.Type.TODAYS_STATS)))
                        .thenReturn(flowOf(listOf(TODAYS_STATS_ENTITY)))

                val result = cardsStore.getCards(siteModel, listOf(CardModel.Type.TODAYS_STATS)).single()

                assertThat(result).isEqualTo(CardsResult(listOf(TODAYS_STATS_MODEL)))
            }

    @Test
    fun `when get cards gets triggered for posts only, then a flow of post card model is returned`() = test {
        whenever(dao.get(SITE_LOCAL_ID, listOf(CardModel.Type.POSTS))).thenReturn(flowOf(listOf(POSTS_ENTITY)))

        val result = cardsStore.getCards(siteModel, listOf(CardModel.Type.POSTS)).single()

        assertThat(result).isEqualTo(CardsResult(listOf(POSTS_MODEL)))
    }
}
