package org.wordpress.android.fluxc.store.dashboard

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.ActivityCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardRowModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel.PageCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.FetchCardsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PageResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.TodaysStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao.CardEntity
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsPayload
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.FormattableContent
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

/* PAGES */
const val PAGE_ID = 1
const val PAGE_TITLE = "title"
const val PAGE_CONTENT = "content"
const val PAGE_MODIFIED_ON = "2023-03-02 10:26:53"
const val PAGE_STATUS = "publish"
const val PAGE_DATE = "2023-03-02 10:30:53"

/* DYNAMIC CARDS */
const val DYNAMIC_CARD_ID = "year_in_review_2023"
const val DYNAMIC_CARD_TITLE = "News"
const val DYNAMIC_CARD_FEATURED_IMAGE = "https://path/to/image"
const val DYNAMIC_CARD_URL = "https://wordpress.com"
const val DYNAMIC_CARD_ACTION = "Call to action"
const val DYNAMIC_CARD_ORDER = "top"
const val DYNAMIC_CARD_ROW_ICON = "https://path/to/image"
const val DYNAMIC_CARD_ROW_TITLE = "Row title"
const val DYNAMIC_CARD_ROW_DESCRIPTION = "Row description"

/* ACTIVITY */
const val ACTIVITY_ID = "activity123"
const val ACTIVITY_SUMMARY = "activity"
const val ACTIVITY_NAME = "name"
const val ACTIVITY_TYPE = "create a blog"
const val ACTIVITY_IS_REWINDABLE = false
const val ACTIVITY_REWIND_ID = "10.0"
const val ACTIVITY_GRID_ICON = "gridicon.jpg"
const val ACTIVITY_STATUS = "OK"
const val ACTIVITY_ACTOR_TYPE = "author"
const val ACTIVITY_ACTOR_NAME = "John Smith"
const val ACTIVITY_ACTOR_EXTERNAL_USER_ID = 10L
const val ACTIVITY_ACTOR_WPCOM_USER_ID = 15L
const val ACTIVITY_ACTOR_ROLE = "admin"
const val ACTIVITY_ACTOR_ICON_URL = "dog.jpg"
const val ACTIVITY_PUBLISHED_DATE = "2021-12-27 11:33:55"
const val ACTIVITY_CONTENT = "content"

private const val BUILD_NUMBER_PARAM = "build_number_param"
private const val DEVICE_ID_PARAM = "device_id_param"
private const val IDENTIFIER_PARAM = "identifier_param"
private const val MARKETING_VERSION_PARAM = "marketing_version_param"
private const val PLATFORM_PARAM = "platform_param"
private const val ANDROID_VERSION_PARAM = "14.0"

/* CARD TYPES */

private val CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS,
    CardModel.Type.POSTS,
    CardModel.Type.PAGES,
    CardModel.Type.ACTIVITY,
    CardModel.Type.DYNAMIC,
)

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

private val PAGE_RESPONSE = PageResponse(
    id = PAGE_ID,
    title = PAGE_TITLE,
    content = PAGE_CONTENT,
    modified = PAGE_MODIFIED_ON,
    status = PAGE_STATUS,
    date = PAGE_DATE
)

private val PAGES_RESPONSE = listOf(PAGE_RESPONSE)

private val DYNAMIC_CARD_ROW_RESPONSE = CardsRestClient.DynamicCardRowResponse(
    icon = DYNAMIC_CARD_ROW_ICON,
    title = DYNAMIC_CARD_ROW_TITLE,
    description = DYNAMIC_CARD_ROW_DESCRIPTION
)

private val DYNAMIC_CARD_ROWS_RESPONSE = listOf(DYNAMIC_CARD_ROW_RESPONSE)

private val DYNAMIC_CARD_RESPONSE = CardsRestClient.DynamicCardResponse(
    id = DYNAMIC_CARD_ID,
    title = DYNAMIC_CARD_TITLE,
    featuredImage = DYNAMIC_CARD_FEATURED_IMAGE,
    url = DYNAMIC_CARD_URL,
    action = DYNAMIC_CARD_ACTION,
    order = DYNAMIC_CARD_ORDER,
    rows = DYNAMIC_CARD_ROWS_RESPONSE,
)

private val DYNAMIC_CARDS_RESPONSE = listOf(DYNAMIC_CARD_RESPONSE)

private val ACTIVITY_RESPONSE_ICON = ActivitiesResponse.Icon("jpg", ACTIVITY_ACTOR_ICON_URL, 100, 100)
private val ACTIVITY_RESPONSE_ACTOR = ActivitiesResponse.Actor(
    ACTIVITY_ACTOR_TYPE,
    ACTIVITY_ACTOR_NAME,
    ACTIVITY_ACTOR_EXTERNAL_USER_ID,
    ACTIVITY_ACTOR_WPCOM_USER_ID,
    ACTIVITY_RESPONSE_ICON,
    ACTIVITY_ACTOR_ROLE
)
private val ACTIVITY_RESPONSE_GENERATOR = ActivitiesResponse.Generator(10.3f, 123)
private val ACTIVITY_RESPONSE_PAGE = ActivitiesResponse.ActivityResponse(
    summary = ACTIVITY_SUMMARY,
    content = FormattableContent(text = ACTIVITY_CONTENT),
    name = ACTIVITY_NAME,
    actor = ACTIVITY_RESPONSE_ACTOR,
    type = ACTIVITY_TYPE,
    published = CardsUtils.fromDate(ACTIVITY_PUBLISHED_DATE),
    generator = ACTIVITY_RESPONSE_GENERATOR,
    is_rewindable = ACTIVITY_IS_REWINDABLE,
    rewind_id = ACTIVITY_REWIND_ID,
    gridicon = ACTIVITY_GRID_ICON,
    status = ACTIVITY_STATUS,
    activity_id = ACTIVITY_ID
)

private val ACTIVITY_RESPONSE_ACTIVITIES_PAGE = ActivitiesResponse.Page(orderedItems = listOf(ACTIVITY_RESPONSE_PAGE))
private val ACTIVITY_RESPONSE = ActivitiesResponse(
    totalItems = 1,
    summary = "response",
    current = ACTIVITY_RESPONSE_ACTIVITIES_PAGE
)

private val CARDS_RESPONSE = CardsResponse(
        todaysStats = TODAYS_STATS_RESPONSE,
        posts = POSTS_RESPONSE,
        pages = PAGES_RESPONSE,
        activity = ACTIVITY_RESPONSE,
        dynamic = DYNAMIC_CARDS_RESPONSE,
)

/* MODEL */
private val TODAYS_STATS_MODEL = TodaysStatsCardModel(
        views = TODAYS_STATS_VIEWS,
        visitors = TODAYS_STATS_VISITORS,
        likes = TODAYS_STATS_LIKES,
        comments = TODAYS_STATS_COMMENTS
)

private val TODAYS_STATS_WITH_ERROR_MODEL = TodaysStatsCardModel(
        error = TodaysStatsCardError(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)
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

private val POSTS_WITH_ERROR_MODEL = PostsCardModel(
        error = PostCardError(PostCardErrorType.UNAUTHORIZED)
)

private val PAGE_MODEL = PageCardModel(
        id = PAGE_ID,
        title = PAGE_TITLE,
        content = PAGE_CONTENT,
        lastModifiedOrScheduledOn = CardsUtils.fromDate(PAGE_MODIFIED_ON),
        status = PAGE_STATUS,
        date = CardsUtils.fromDate(PAGE_DATE)
)

private val PAGES_MODEL = PagesCardModel(
        pages = listOf(PAGE_MODEL)
)

private val DYNAMIC_CARD_ROW_MODEL = DynamicCardRowModel(
    icon = DYNAMIC_CARD_ROW_ICON,
    title = DYNAMIC_CARD_ROW_TITLE,
    description = DYNAMIC_CARD_ROW_DESCRIPTION
)

private val DYNAMIC_CARD_MODEL = DynamicCardModel(
    id = DYNAMIC_CARD_ID,
    title = DYNAMIC_CARD_TITLE,
    featuredImage = DYNAMIC_CARD_FEATURED_IMAGE,
    url = DYNAMIC_CARD_URL,
    action = DYNAMIC_CARD_ACTION,
    order = CardOrder.fromString(DYNAMIC_CARD_ORDER),
    rows = listOf(DYNAMIC_CARD_ROW_MODEL)
)

private val DYNAMIC_CARDS_MODEL = DynamicCardsModel(
    dynamicCards = listOf(DYNAMIC_CARD_MODEL)
)

private val ACTIVITY_LOG_MODEL = ActivityLogModel(
    summary = ACTIVITY_SUMMARY,
    content = FormattableContent(text = ACTIVITY_CONTENT),
    name = ACTIVITY_NAME,
    actor = ActivityLogModel.ActivityActor(
        displayName = ACTIVITY_ACTOR_NAME,
        type = ACTIVITY_ACTOR_TYPE,
        wpcomUserID = ACTIVITY_ACTOR_WPCOM_USER_ID,
        avatarURL = ACTIVITY_ACTOR_ICON_URL,
        role = ACTIVITY_ACTOR_ROLE,
    ),
    type = ACTIVITY_TYPE,
    published = CardsUtils.fromDate(ACTIVITY_PUBLISHED_DATE),
    rewindable = ACTIVITY_IS_REWINDABLE,
    rewindID = ACTIVITY_REWIND_ID,
    gridicon = ACTIVITY_GRID_ICON,
    status = ACTIVITY_STATUS,
    activityID = ACTIVITY_ID
)

private val ACTIVITY_CARD_MODEL = ActivityCardModel(
    activities = listOf(ACTIVITY_LOG_MODEL)
)

private val ACTIVITY_CARD_WITH_ERROR_MODEL = ActivityCardModel(
    error = ActivityCardError(ActivityCardErrorType.UNAUTHORIZED)
)

private val CARDS_MODEL = listOf(
        TODAYS_STATS_MODEL,
        POSTS_MODEL,
        PAGES_MODEL,
        ACTIVITY_CARD_MODEL,
        DYNAMIC_CARDS_MODEL,
)

/* ENTITY */
private val TODAYS_STATS_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.TODAYS_STATS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(TODAYS_STATS_MODEL)
)

private val TODAY_STATS_WITH_ERROR_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.TODAYS_STATS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(TODAYS_STATS_WITH_ERROR_MODEL)
)

private val POSTS_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.POSTS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(POSTS_MODEL)
)

private val POSTS_WITH_ERROR_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.POSTS.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(POSTS_WITH_ERROR_MODEL)
)

private val PAGES_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.PAGES.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(PAGES_MODEL)
)

private val DYNAMIC_CARDS_ENTITY = CardEntity(
    siteLocalId = SITE_LOCAL_ID,
    type = CardModel.Type.DYNAMIC.name,
    date = CardsUtils.getInsertDate(),
    json = CardsUtils.GSON.toJson(DYNAMIC_CARDS_MODEL)
)

private val ACTIVITY_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.ACTIVITY.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(ACTIVITY_CARD_MODEL)
)

private val ACTIVITY_WITH_ERROR_ENTITY = CardEntity(
        siteLocalId = SITE_LOCAL_ID,
        type = CardModel.Type.ACTIVITY.name,
        date = CardsUtils.getInsertDate(),
        json = CardsUtils.GSON.toJson(ACTIVITY_CARD_WITH_ERROR_MODEL)
)

private val CARDS_ENTITY = listOf(
        TODAYS_STATS_ENTITY,
        POSTS_ENTITY,
        PAGES_ENTITY,
        ACTIVITY_ENTITY,
        DYNAMIC_CARDS_ENTITY,
)

@RunWith(MockitoJUnitRunner::class)
class CardsStoreTest {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: CardsRestClient
    @Mock private lateinit var dao: CardsDao
    @Mock private lateinit var cardsRespone: CardsResponse

    private lateinit var defaultFetchCardsPayload: FetchCardsPayload
    private lateinit var cardsStore: CardsStore

    @Before
    fun setUp() {
        cardsStore = CardsStore(
                restClient,
                dao,
                initCoroutineEngine()
        )
        setUpMocks()
        defaultFetchCardsPayload = FetchCardsPayload(
            siteModel,
            CARD_TYPES,
            BUILD_NUMBER_PARAM,
            DEVICE_ID_PARAM,
            IDENTIFIER_PARAM,
            MARKETING_VERSION_PARAM,
            PLATFORM_PARAM,
            ANDROID_VERSION_PARAM,
        )
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
    }

    @Test
    fun `given all card types, when fetch cards triggered, then all cards model is inserted into db`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        cardsStore.fetchCards(defaultFetchCardsPayload)

        verify(dao).insertWithDate(siteModel.id, CARDS_MODEL)
    }

    @Test
    fun `given todays stats type, when fetch cards triggered, then today's stats card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(todaysStats = TODAYS_STATS_RESPONSE))
        whenever(
            restClient.fetchCards(
                FetchCardsPayload(
                    siteModel,
                    listOf(CardModel.Type.TODAYS_STATS),
                    BUILD_NUMBER_PARAM,
                    DEVICE_ID_PARAM,
                    IDENTIFIER_PARAM,
                    MARKETING_VERSION_PARAM,
                    PLATFORM_PARAM,
                    ANDROID_VERSION_PARAM,
                )
            )
        ).thenReturn(payload)

        cardsStore.fetchCards(
            FetchCardsPayload(
                siteModel,
                listOf(CardModel.Type.TODAYS_STATS),
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
        )

        verify(dao).insertWithDate(siteModel.id, listOf(TODAYS_STATS_MODEL))
    }

    @Test
    fun `given posts type, when fetch cards triggered, then post card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(posts = POSTS_RESPONSE))
        whenever(
            restClient.fetchCards(
                FetchCardsPayload(
                    siteModel,
                    listOf(CardModel.Type.POSTS),
                    BUILD_NUMBER_PARAM,
                    DEVICE_ID_PARAM,
                    IDENTIFIER_PARAM,
                    MARKETING_VERSION_PARAM,
                    PLATFORM_PARAM,
                    ANDROID_VERSION_PARAM,
                )
            )
        ).thenReturn(payload)

        cardsStore.fetchCards(
            FetchCardsPayload(
                siteModel,
                listOf(CardModel.Type.POSTS),
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
        )

        verify(dao).insertWithDate(siteModel.id, listOf(POSTS_MODEL))
    }

    @Test
    fun `given pages type, when fetch cards triggered, then pages card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(pages = PAGES_RESPONSE))
        whenever(
            restClient.fetchCards(
                FetchCardsPayload(
                    siteModel,
                    listOf(CardModel.Type.PAGES),
                    BUILD_NUMBER_PARAM,
                    DEVICE_ID_PARAM,
                    IDENTIFIER_PARAM,
                    MARKETING_VERSION_PARAM,
                    PLATFORM_PARAM,
                    ANDROID_VERSION_PARAM,
                )
            )
        ).thenReturn(payload)

        cardsStore.fetchCards(
            FetchCardsPayload(
                siteModel,
                listOf(CardModel.Type.PAGES),
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
        )

        verify(dao).insertWithDate(siteModel.id, listOf(PAGES_MODEL))
    }

    @Test
    fun `given dynamic cards type, when fetch cards triggered, then dynamic cards model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(dynamic = DYNAMIC_CARDS_RESPONSE))
        whenever(
            restClient.fetchCards(
                FetchCardsPayload(
                    siteModel,
                    listOf(CardModel.Type.DYNAMIC),
                    BUILD_NUMBER_PARAM,
                    DEVICE_ID_PARAM,
                    IDENTIFIER_PARAM,
                    MARKETING_VERSION_PARAM,
                    PLATFORM_PARAM,
                    ANDROID_VERSION_PARAM,
                )
            )
        ).thenReturn(payload)

        cardsStore.fetchCards(
            FetchCardsPayload(
                siteModel,
                listOf(CardModel.Type.DYNAMIC),
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
        )

        verify(dao).insertWithDate(siteModel.id, listOf(DYNAMIC_CARDS_MODEL))
    }

    @Test
    fun `given activity type, when fetch cards triggered, then activity card model inserted into db`() = test {
        val payload = CardsPayload(CardsResponse(activity = ACTIVITY_RESPONSE))
        whenever(
            restClient.fetchCards(
                FetchCardsPayload(
                    siteModel,
                    listOf(CardModel.Type.ACTIVITY),
                    BUILD_NUMBER_PARAM,
                    DEVICE_ID_PARAM,
                    IDENTIFIER_PARAM,
                    MARKETING_VERSION_PARAM,
                    PLATFORM_PARAM,
                    ANDROID_VERSION_PARAM,
                )
            )
        ).thenReturn(payload)

        cardsStore.fetchCards(
            FetchCardsPayload(
                siteModel,
                listOf(CardModel.Type.ACTIVITY),
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
        )

        verify(dao).insertWithDate(siteModel.id, listOf(ACTIVITY_CARD_MODEL))
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then empty cards model is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        val result = cardsStore.fetchCards(defaultFetchCardsPayload)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given card response with exception, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)
        whenever(dao.insertWithDate(siteModel.id, CARDS_MODEL)).thenThrow(IllegalStateException("Error"))

        val result = cardsStore.fetchCards(defaultFetchCardsPayload)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.GENERIC_ERROR, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given cards error, when fetch cards gets triggered, then cards error is returned`() = test {
        val errorType = CardsErrorType.API_ERROR
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        val result = cardsStore.fetchCards(defaultFetchCardsPayload)

        assertThat(result.model).isNull()
        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given authorization required, when fetch cards gets triggered, then db is cleared of cards model`() = test {
        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        cardsStore.fetchCards(defaultFetchCardsPayload)

        verify(dao).clear()
    }

    @Test
    fun `given authorization required, when fetch cards gets triggered, then empty cards model is returned`() = test {
        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        val result = cardsStore.fetchCards(defaultFetchCardsPayload)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given empty cards payload, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = CardsPayload<CardsResponse>()
        whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(payload)

        val result = cardsStore.fetchCards(defaultFetchCardsPayload)

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

    /* TODAYS STATS CARD WITH ERROR */

    @Test
    fun `given todays stats card with error, when fetch cards triggered, then card with error inserted into db`() =
            test {
                whenever(restClient.fetchCards(defaultFetchCardsPayload)).thenReturn(
                    CardsPayload(
                        cardsRespone
                    )
                )
                whenever(cardsRespone.toCards()).thenReturn(listOf(TODAYS_STATS_WITH_ERROR_MODEL))

                cardsStore.fetchCards(defaultFetchCardsPayload)

                verify(dao).insertWithDate(siteModel.id, listOf(TODAYS_STATS_WITH_ERROR_MODEL))
            }

    @Test
    fun `given today's stats jetpack disconn error, when get cards triggered, then error exists in the card`() = test {
        whenever(dao.get(SITE_LOCAL_ID))
                .thenReturn(
                        flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)))
                )

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)
    }

    @Test
    fun `given today's stats jetpack disabled error, when get cards triggered, then error exists in the card`() = test {
        whenever(dao.get(SITE_LOCAL_ID))
                .thenReturn(flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.JETPACK_DISABLED))))

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.JETPACK_DISABLED)
    }

    @Test
    fun `given today's stats jetpack unauth error, when get cards triggered, then error exists in the card`() = test {
        whenever(dao.get(SITE_LOCAL_ID))
                .thenReturn(flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.UNAUTHORIZED))))

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.UNAUTHORIZED)
    }

    /* POSTS CARD WITH ERROR */
    @Test
    fun `given posts card with error, when fetch cards triggered, then card with error inserted into db`() = test {
        whenever(
            restClient.fetchCards(defaultFetchCardsPayload)
        ).thenReturn(CardsPayload(cardsRespone))
        whenever(cardsRespone.toCards()).thenReturn(listOf(POSTS_WITH_ERROR_MODEL))

        cardsStore.fetchCards(defaultFetchCardsPayload)

        verify(dao).insertWithDate(siteModel.id, listOf(POSTS_WITH_ERROR_MODEL))
    }

    @Test
    fun `given posts card unauth error, when get cards triggered, then error exists in the card`() = test {
        whenever(dao.get(SITE_LOCAL_ID)).thenReturn(flowOf(listOf(POSTS_WITH_ERROR_ENTITY)))

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result.findPostsCardError()?.type).isEqualTo(PostCardErrorType.UNAUTHORIZED)
    }

    /* ACTIVITY CARD WITH ERROR */
    @Test
    fun `given activity unauth error, when get cards triggered, then error exists in the card`() = test {
        whenever(dao.get(SITE_LOCAL_ID))
            .thenReturn(flowOf(listOf(getActivityErrorCardEntity())))

        val result = cardsStore.getCards(siteModel).single()

        assertThat(result.findActivityCardError()?.type).isEqualTo(ActivityCardErrorType.UNAUTHORIZED)
    }

    private fun CardsResult<List<CardModel>>.findTodaysStatsCardError(): TodaysStatsCardError? =
            model?.filterIsInstance(TodaysStatsCardModel::class.java)?.firstOrNull()?.error

    private fun CardsResult<List<CardModel>>.findPostsCardError(): PostCardError? =
            model?.filterIsInstance(PostsCardModel::class.java)?.firstOrNull()?.error

    private fun CardsResult<List<CardModel>>.findActivityCardError(): ActivityCardError? =
        model?.filterIsInstance(ActivityCardModel::class.java)?.firstOrNull()?.error

    private fun getTodaysStatsErrorCardEntity(type: TodaysStatsCardErrorType) =
        TODAY_STATS_WITH_ERROR_ENTITY.copy(
            json = CardsUtils.GSON.toJson(TodaysStatsCardModel(error = TodaysStatsCardError(type)))
        )

    private fun getActivityErrorCardEntity() =
        ACTIVITY_WITH_ERROR_ENTITY.copy(
            json = CardsUtils.GSON.toJson(
                ActivityCardModel(
                    error = ActivityCardError(
                        ActivityCardErrorType.UNAUTHORIZED
                    )
                )
            )
        )
}
