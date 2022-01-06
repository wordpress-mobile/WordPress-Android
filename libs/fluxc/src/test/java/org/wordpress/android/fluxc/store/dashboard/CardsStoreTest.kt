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
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
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

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
const val POST_FEATURED_IMAGE = "featuredImage"
const val POST_DATE = "2021-12-27 11:33:55"

/* RESPONSE */

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
        posts = POSTS_RESPONSE
)

/* MODEL */

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
        POSTS_MODEL
)

/* ENTITY */

private val POSTS_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.POSTS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(POSTS_MODEL)
)

private val CARDS_ENTITY = listOf(
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
    fun `given cards response, when fetch cards gets triggered, then cards model is inserted into db`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        cardsStore.fetchCards(siteModel)

        verify(dao).insertWithDate(siteModel.id, CARDS_MODEL)
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then empty cards model is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given card response with exception, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)
        whenever(dao.insertWithDate(siteModel.id, CARDS_MODEL)).thenThrow(IllegalStateException("Error"))

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.GENERIC_ERROR, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given cards error, when fetch cards gets triggered, then cards error is returned`() = test {
        val errorType = CardsErrorType.API_ERROR
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given empty cards payload, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload<CardsResponse>()
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.INVALID_RESPONSE, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `when get cards gets triggered, then a flow of cards model is returned`() = test {
        whenever(dao.get(SITE_LOCAL_ID)).thenReturn(flowOf(CARDS_ENTITY))

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result).isEqualTo(CardsResult(CARDS_MODEL))
    }
}
