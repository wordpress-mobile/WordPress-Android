package org.wordpress.android.ui.mysite.cards.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardRowModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel.PageCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.FetchCardsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityLogCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.activity.DashboardActivityLogCardFeatureUtils
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostsCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsViewModelSlice
import org.wordpress.android.ui.mysite.cards.dynamiccard.DynamicCardsViewModelSlice
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.DynamicDashboardCardsFeatureConfig
import org.wordpress.android.viewmodel.Event

/* SITE */

const val SITE_LOCAL_ID = 1

/* STATS */

const val STATS_VIEWS = 100
const val STATS_VISITORS = 30
const val STATS_LIKES = 50
const val STATS_COMMENTS = 10

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
const val ACTIVITY_ACTOR_WPCOM_USER_ID = 15L
const val ACTIVITY_ACTOR_ROLE = "admin"
const val ACTIVITY_ACTOR_ICON_URL = "dog.jpg"
const val ACTIVITY_PUBLISHED_DATE = "2021-12-27 11:33:55"
const val ACTIVITY_CONTENT = "content"

private const val BUILD_NUMBER_PARAM = "12345"
private const val DEVICE_ID_PARAM = "device_id_param"
private const val IDENTIFIER_PARAM = "identifier_param"
private const val MARKETING_VERSION_PARAM = "marketing_version_param"
private const val PLATFORM_PARAM = "android"
private const val ANDROID_VERSION_PARAM = "14.0"

/* MODEL */

private val TODAYS_STATS_CARDS_MODEL = TodaysStatsCardModel(
    views = STATS_VIEWS,
    visitors = STATS_VISITORS,
    likes = STATS_LIKES,
    comments = STATS_COMMENTS
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

private val ACTIVITY_CARD_MODEL = CardModel.ActivityCardModel(
    activities = listOf(ACTIVITY_LOG_MODEL)
)

private val CARDS_MODEL: List<CardModel> = listOf(
    TODAYS_STATS_CARDS_MODEL,
    POSTS_MODEL,
    PAGES_MODEL,
    ACTIVITY_CARD_MODEL,
    DYNAMIC_CARDS_MODEL,
)

private val DEFAULT_CARD_TYPE = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)
private val PAGES_FEATURED_ENABLED_CARD_TYPE =
    listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.PAGES, CardModel.Type.POSTS)
private val ACTIVITY_FEATURED_ENABLED_CARD_TYPE =
    listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.ACTIVITY, CardModel.Type.POSTS)
private val DYNAMIC_CARDS_ENABLED_CARD_TYPE =
    listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS, CardModel.Type.DYNAMIC)

@ExperimentalCoroutinesApi
class CardsViewModelSliceTest : BaseUnitTest() {
    @Mock
    private lateinit var cardsStore: CardsStore

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var dashboardActivityLogCardFeatureUtils: DashboardActivityLogCardFeatureUtils

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var dynamicDashboardCardsFeatureConfig: DynamicDashboardCardsFeatureConfig

    @Mock
    private lateinit var pagesCardViewModelSlice: PagesCardViewModelSlice

    @Mock
    private lateinit var dynamicCardsViewModelSlice: DynamicCardsViewModelSlice

    @Mock
    private lateinit var todaysStatsViewModelSlice: TodaysStatsViewModelSlice

    @Mock
    private lateinit var postsCardViewModelSlice: PostsCardViewModelSlice

    @Mock
    private lateinit var activityLogCardViewModelSlice: ActivityLogCardViewModelSlice

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var preferenceUtilsWrapper: PreferenceUtilsWrapper

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var cardTracker: CardsTracker

    private lateinit var viewModelSlice: CardViewModelSlice

    private lateinit var defaultFetchCardsPayload: FetchCardsPayload

    private val data = CardsResult(
        model = CARDS_MODEL
    )
    private val success = CardsResult<List<CardModel>>()
    private val apiError = CardsResult<List<CardModel>>(
        error = CardsError(CardsErrorType.API_ERROR)
    )

    private lateinit var result: List<CardsState>

    private lateinit var isRefreshing: List<Boolean>

    private lateinit var refresh: List<Event<Boolean>>

    @Before
    fun setUp() {
        whenever(dynamicCardsViewModelSlice.topDynamicCards).thenReturn(MutableLiveData())
        whenever(dynamicCardsViewModelSlice.bottomDynamicCards).thenReturn(MutableLiveData())
        whenever(todaysStatsViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(pagesCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(postsCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(activityLogCardViewModelSlice.uiModel).thenReturn(MutableLiveData())

        viewModelSlice = CardViewModelSlice(
            cardsStore,
            dashboardActivityLogCardFeatureUtils,
            testDispatcher(),
            appPrefsWrapper,
            dynamicDashboardCardsFeatureConfig,
            pagesCardViewModelSlice,
            dynamicCardsViewModelSlice,
            todaysStatsViewModelSlice,
            postsCardViewModelSlice,
            activityLogCardViewModelSlice,
            preferenceUtilsWrapper,
            buildConfigWrapper,
            cardTracker
        )
        defaultFetchCardsPayload = FetchCardsPayload(
            siteModel,
            DEFAULT_CARD_TYPE,
            BUILD_NUMBER_PARAM,
            DEVICE_ID_PARAM,
            IDENTIFIER_PARAM,
            MARKETING_VERSION_PARAM,
            PLATFORM_PARAM,
            ANDROID_VERSION_PARAM,
        )

        viewModelSlice.initialize(testScope())

        result = emptyList()
        viewModelSlice.uiModel.observeForever { result = result + it }

        isRefreshing = emptyList()
        viewModelSlice.isRefreshing.observeForever { isRefreshing = isRefreshing + it }

        refresh = emptyList()
        viewModelSlice.refresh.observeForever { refresh = refresh + it }
    }

    private fun setUpMocks(
        isDashboardCardActivityLogEnabled: Boolean = false,
        isRequestPages: Boolean = false,
        isPagesCardHidden: Boolean = false,
        isTodaysStatsCardHidden: Boolean = false,
        isDynamicCardsEnabled: Boolean = false,
    ) {
        whenever(dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel))
            .thenReturn(isDashboardCardActivityLogEnabled)
        whenever(siteModel.hasCapabilityEditPages).thenReturn(isRequestPages)
        whenever(appPrefsWrapper.getShouldHidePagesDashboardCard(any()))
            .thenReturn(isPagesCardHidden)
        whenever(appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(any()))
            .thenReturn(isTodaysStatsCardHidden)
        whenever(dynamicDashboardCardsFeatureConfig.isEnabled())
            .thenReturn(isDynamicCardsEnabled)
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(BUILD_NUMBER_PARAM.toInt())
        whenever(buildConfigWrapper.getApplicationId()).thenReturn(IDENTIFIER_PARAM)
        whenever(buildConfigWrapper.getAppVersionName()).thenReturn(MARKETING_VERSION_PARAM)
        whenever(buildConfigWrapper.androidVersion).thenReturn(ANDROID_VERSION_PARAM)
        whenever(preferenceUtilsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)
        whenever(sharedPreferences.getString(any(), anyOrNull())).thenReturn(DEVICE_ID_PARAM)
    }

    /* GET DATA */

    @Test
    fun `when build is invoked, then start collecting cards from store (database)`() = test {
        setUpMocks()

        viewModelSlice.buildCard(siteModel)

        verify(cardsStore).getCards(siteModel)
    }

    @Test
    fun `when build is invoked, then todays stats is from store(db)`() = test {
        setUpMocks()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))

        viewModelSlice.buildCard(siteModel)

        val getCardsResult = cardsStore.getCards(siteModel).single().model
        assertThat(getCardsResult?.any { it is TodaysStatsCardModel }).isTrue
    }

    @Test
    fun `given build is invoked, when cards are collected, then data is loaded (database)`() = test {
        setUpMocks(
            isDashboardCardActivityLogEnabled = true,
            isRequestPages = true,
            isDynamicCardsEnabled = true
        )
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))

        viewModelSlice.buildCard(siteModel)

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isInstanceOf(CardsState.Success::class.java)
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then cards are fetched from store (network)`() = test {
        setUpMocks()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(CARDS_MODEL)))

        viewModelSlice.buildCard(siteModel)
        advanceUntilIdle()

        verify(cardsStore).fetchCards(defaultFetchCardsPayload)
    }

    @Test
    fun `given no error, when build is invoked, then data loaded from get cards (database)`() = test {
        setUpMocks(
            isDashboardCardActivityLogEnabled = true,
            isRequestPages = true,
            isDynamicCardsEnabled = true
        )
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))

        viewModelSlice.buildCard(siteModel)

        assertThat(result.first()).isInstanceOf(CardsState.Success::class.java)
    }

    @Test
    fun `when refresh is invoked, then todays stats are requested from network`() =
        test {
            setUpMocks()
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)

            advanceUntilIdle()

            verify(cardsStore).fetchCards(defaultFetchCardsPayload)
        }

    @Test
    fun `given error, when build is invoked, then error message is shown (network)`() = test {
        setUpMocks()
        val testData = CardsResult(model = listOf(TODAYS_STATS_CARDS_MODEL, POSTS_MODEL))
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(model = testData.model)))
        whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(apiError)
        viewModelSlice.refresh.observeForever { }

        viewModelSlice.buildCard(siteModel)

        advanceUntilIdle()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.last()).isInstanceOf(CardsState.ErrorState::class.java)
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get cards (database)`() = test {
        setUpMocks()
        val filteredData = CardsResult(model = data.model?.filterIsInstance<PostsCardModel>()?.toList())
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(model = filteredData.model)))
        whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(success).thenReturn(success)

        viewModelSlice.buildCard(siteModel)

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isInstanceOf(CardsState.Success::class.java)
    }

    /* IS REFRESHING */

    @Test
    fun `when build is invoked, then refresh is set to true`() = test {
        setUpMocks()

        viewModelSlice.buildCard(siteModel)

        assertThat(isRefreshing.size).isEqualTo(2)
        assertThat(isRefreshing.first()).isTrue()
    }

    @Test
    fun `when build is invoked, then refresh is set to true and then to false`() = test {
        setUpMocks()
        val testData = CardsResult(model = listOf(TODAYS_STATS_CARDS_MODEL, POSTS_MODEL))
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(model = testData.model)))
        whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(success)

        viewModelSlice.buildCard(siteModel)
        advanceUntilIdle()

        assertThat(isRefreshing.size).isEqualTo(4)
        assertThat(isRefreshing[0]).isTrue // init
        assertThat(isRefreshing[1]).isFalse // build(...) -> refresh()
        assertThat(isRefreshing[2]).isTrue // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(isRefreshing[3]).isFalse // refresh()
    }

    @Test
    fun `given error, when data has been refreshed, then refresh is set to false`() = test {
        setUpMocks()
        val testData = CardsResult(model = listOf(TODAYS_STATS_CARDS_MODEL, POSTS_MODEL))
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(model = testData.model)))
        whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(apiError)

        viewModelSlice.buildCard(siteModel)
        advanceUntilIdle()

        assertThat(isRefreshing.size).isEqualTo(4)
        assertThat(isRefreshing[0]).isTrue // init
        assertThat(isRefreshing[1]).isFalse // build(...) -> refresh()
        assertThat(isRefreshing[2]).isTrue // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(isRefreshing[3]).isFalse // refresh()
    }

    /* INVALID SITE */

    @Test
    fun `given invalid site, when build is invoked, then error card is shown`() = test {
        setUpMocks(isRequestPages = true)
        val fetchCardsPayload = FetchCardsPayload(
            siteModel,
            PAGES_FEATURED_ENABLED_CARD_TYPE,
            BUILD_NUMBER_PARAM,
            DEVICE_ID_PARAM,
            IDENTIFIER_PARAM,
            MARKETING_VERSION_PARAM,
            PLATFORM_PARAM,
            ANDROID_VERSION_PARAM,
        )
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult()))
        whenever(cardsStore.fetchCards(fetchCardsPayload)).thenReturn(apiError)

        viewModelSlice.buildCard(siteModel)
        testScope().advanceUntilIdle()

        assertThat(result.last()).isInstanceOf(CardsState.ErrorState::class.java)
    }

    @Test
    fun `given pages feature enabled + card not hidden, when refresh is invoked, then pages requested from network`() =
        test {
            setUpMocks(isRequestPages = true)
            val fetchCardsPayload = FetchCardsPayload(
                siteModel,
                PAGES_FEATURED_ENABLED_CARD_TYPE,
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(fetchCardsPayload)).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(fetchCardsPayload)
        }

    @Test
    fun `given pages feature enabled + card hidden, when refresh is invoked, then pages not requested from network`() =
        test {
            setUpMocks(isRequestPages = true, isPagesCardHidden = true)
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(defaultFetchCardsPayload)
        }

    @Test
    fun `given activity feature enabled, when refresh is invoked, then activity are requested from network`() =
        test {
            setUpMocks(isDashboardCardActivityLogEnabled = true)
            val fetchCardsPayload = FetchCardsPayload(
                siteModel,
                ACTIVITY_FEATURED_ENABLED_CARD_TYPE,
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(fetchCardsPayload)).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(fetchCardsPayload)
        }

    @Test
    fun `given dynamic cards are enabled, when refresh is invoked, then dynamic cards are requested from network`() =
        test {
            setUpMocks(isDynamicCardsEnabled = true)
            val fetchCardsPayload = FetchCardsPayload(
                siteModel,
                DYNAMIC_CARDS_ENABLED_CARD_TYPE,
                BUILD_NUMBER_PARAM,
                DEVICE_ID_PARAM,
                IDENTIFIER_PARAM,
                MARKETING_VERSION_PARAM,
                PLATFORM_PARAM,
                ANDROID_VERSION_PARAM,
            )
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(fetchCardsPayload)).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(fetchCardsPayload)
        }

    @Test
    fun `given activity feature disabled, when refresh is invoked, then activity not requested`() =
        test {
            setUpMocks(isDashboardCardActivityLogEnabled = false)
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(success).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(defaultFetchCardsPayload)
        }

    @Test
    fun `given stats card not hidden, when refresh is invoked, then stats requested from network`() =
        test {
            setUpMocks(isTodaysStatsCardHidden = false)
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(defaultFetchCardsPayload)).thenReturn(success)
            viewModelSlice.refresh.observeForever { }

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(defaultFetchCardsPayload)
        }

    @Test
    fun `given stats card is hidden, when refresh is invoked, then stats not requested from network`() =
        test {
            setUpMocks(isTodaysStatsCardHidden = true)
            whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))

            viewModelSlice.buildCard(siteModel)
            advanceUntilIdle()

            verify(cardsStore).fetchCards(
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
        }
}
