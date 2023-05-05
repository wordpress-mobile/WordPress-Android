package org.wordpress.android.ui.mysite.cards.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel.PageCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.activity.DashboardActivityLogCardFeatureUtils
import org.wordpress.android.util.config.DashboardCardPagesConfig
import org.wordpress.android.util.config.MySiteDashboardTodaysStatsCardFeatureConfig

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
    ACTIVITY_CARD_MODEL
)

private val DEFAULT_CARD_TYPE = listOf(CardModel.Type.POSTS)
private val STATS_FEATURED_ENABLED_CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)
private val PAGES_FEATURED_ENABLED_CARD_TYPE = listOf(CardModel.Type.PAGES, CardModel.Type.POSTS)
private val ACTIVITY_FEATURED_ENABLED_CARD_TYPE = listOf(CardModel.Type.ACTIVITY, CardModel.Type.POSTS)

@ExperimentalCoroutinesApi
class CardsSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var cardsStore: CardsStore

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var todaysStatsCardFeatureConfig: MySiteDashboardTodaysStatsCardFeatureConfig

    @Mock
    private lateinit var dashboardCardPagesConfig: DashboardCardPagesConfig

    @Mock
    private lateinit var dashboardActivityLogCardFeatureUtils: DashboardActivityLogCardFeatureUtils

    private lateinit var cardSource: CardsSource

    private val data = CardsResult(
        model = CARDS_MODEL
    )
    private val success = CardsResult<List<CardModel>>()
    private val apiError = CardsResult<List<CardModel>>(
        error = CardsError(CardsErrorType.API_ERROR)
    )

    @Before
    fun setUp() {
        init()
    }

    private fun init(
        isTodaysStatsCardFeatureConfigEnabled: Boolean = false,
        isDashboardCardPagesConfigEnabled: Boolean = false,
        isDashboardCardActivityLogEnabled: Boolean = false
    ) {
        setUpMocks(
            isTodaysStatsCardFeatureConfigEnabled,
            isDashboardCardPagesConfigEnabled,
            isDashboardCardActivityLogEnabled
        )
        cardSource = CardsSource(
            selectedSiteRepository,
            cardsStore,
            dashboardActivityLogCardFeatureUtils,
            todaysStatsCardFeatureConfig,
            dashboardCardPagesConfig,
            testDispatcher()
        )
    }

    private fun setUpMocks(
        isTodaysStatsCardFeatureConfigEnabled: Boolean,
        isDashboardCardPagesConfigEnabled: Boolean = false,
        isDashboardCardActivityLogEnabled: Boolean = false
    ) {
        whenever(todaysStatsCardFeatureConfig.isEnabled()).thenReturn(isTodaysStatsCardFeatureConfigEnabled)
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(isDashboardCardPagesConfigEnabled)
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel))
            .thenReturn(isDashboardCardActivityLogEnabled)
    }

    /* GET DATA */

    @Test
    fun `when build is invoked, then start collecting cards from store (database)`() = test {
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(cardsStore).getCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given today's stats feature enabled, when build is invoked, then todays stats from store(database)`() = test {
        init(isTodaysStatsCardFeatureConfigEnabled = true)
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)).thenReturn(flowOf(data))

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)
    }

    @Test
    fun `given build is invoked, when cards are collected, then data is loaded (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data.model))
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then cards are fetched from store (network)`() = test {
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(CardsResult(CARDS_MODEL)))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        advanceUntilIdle()

        verify(cardsStore).fetchCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get cards (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(CardsResult())
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data.model))
    }

    @Test
    fun `given today's stats feature enabled, when refresh is invoked, then todays stats are requested from network`() =
        test {
            init(isTodaysStatsCardFeatureConfigEnabled = true)
            val result = mutableListOf<CardsUpdate>()
            whenever(cardsStore.getCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)).thenReturn(success)
            cardSource.refresh.observeForever { }

            cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
                it?.let { result.add(it) }
            }
            advanceUntilIdle()

            verify(cardsStore).fetchCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)
        }

    @Test
    fun `given error, when build is invoked, then error snackbar with stale message is also shown (network)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(apiError)
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(2)
        assertThat(result[0]).isEqualTo(
            CardsUpdate(
                cards = data.model,
                showSnackbarError = false,
                showStaleMessage = false
            )
        )
        assertThat(result[1]).isEqualTo(
            CardsUpdate(
                cards = data.model,
                showSnackbarError = true,
                showStaleMessage = true
            )
        )
    }

    @Test
    fun `given no error, when refresh is invoked, then data is only loaded from get cards (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(success).thenReturn(success)
        cardSource.refresh.observeForever { }
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data.model))
    }

    @Test
    fun `given error, when refresh is invoked, then error snackbar with stale message also shown (network)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(success).thenReturn(apiError)
        cardSource.refresh.observeForever { }
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        cardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(CardsUpdate(data.model))
        assertThat(result.last()).isEqualTo(
            CardsUpdate(
                cards = data.model,
                showSnackbarError = true,
                showStaleMessage = true
            )
        )
    }

    /* IS REFRESHING */

    @Test
    fun `when build is invoked, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        cardSource.refresh.observeForever { result.add(it) }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(success).thenReturn(success)
        cardSource.refresh.observeForever { result.add(it) }
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        cardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> cardsStore.fetchCards(...) -> success
    }

    @Test
    fun `given no error, when data has been refreshed, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(success)
        cardSource.refresh.observeForever { result.add(it) }
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        cardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> cardsStore.fetchCards(...) -> success
    }

    @Test
    fun `given error, when data has been refreshed, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(apiError)
        cardSource.refresh.observeForever { result.add(it) }
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        cardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> cardsStore.fetchCards(...) -> error
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> cardsStore.fetchCards(...) -> error
    }

    /* INVALID SITE */

    @Test
    fun `given invalid site, when build is invoked, then error card is shown`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), invalidSiteLocalId).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(showErrorCard = true))
    }

    @Test
    fun `given invalid site, when refresh is invoked, then error card is shown`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.refresh.observeForever { }
        cardSource.build(testScope(), invalidSiteLocalId).observeForever {
            result.add(it)
        }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(CardsUpdate(showErrorCard = true))
        assertThat(result.last()).isEqualTo(CardsUpdate(showErrorCard = true))
    }

    @Test
    fun `given pages enabled and site is capable, when build is invoked, then pages from store(database)`() = test {
        init(isDashboardCardPagesConfigEnabled = true)
        whenever(siteModel.hasCapabilityEditPages).thenReturn(true)
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, PAGES_FEATURED_ENABLED_CARD_TYPE)).thenReturn(flowOf(data))

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, PAGES_FEATURED_ENABLED_CARD_TYPE)
    }

    @Test
    fun `given pages enabled and site is not capable, when build is invoked, then pages not requested`() = test {
        init(isDashboardCardPagesConfigEnabled = true)
        val result = mutableListOf<CardsUpdate>()
        whenever(siteModel.hasCapabilityEditPages).thenReturn(false)

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given pages enabled and hosted site not capable, when build is invoked, then pages not requested`() = test {
        init(isDashboardCardPagesConfigEnabled = true)
        val result = mutableListOf<CardsUpdate>()
        whenever(siteModel.isSelfHostedAdmin).thenReturn(false)

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given pages feature enabled, when refresh is invoked, then pages are requested from network`() =
        test {
            init(isDashboardCardPagesConfigEnabled = true)
            val result = mutableListOf<CardsUpdate>()
            whenever(siteModel.hasCapabilityEditPages).thenReturn(true)
            whenever(cardsStore.getCards(siteModel, PAGES_FEATURED_ENABLED_CARD_TYPE)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(siteModel, PAGES_FEATURED_ENABLED_CARD_TYPE)).thenReturn(success)
            cardSource.refresh.observeForever { }

            cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
                it?.let { result.add(it) }
            }
            advanceUntilIdle()

            verify(cardsStore).fetchCards(siteModel, PAGES_FEATURED_ENABLED_CARD_TYPE)
        }

    @Test
    fun `given activity feature enabled, when build is invoked, then activity from store(database)`() = test {
        init(isDashboardCardActivityLogEnabled = true)
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, ACTIVITY_FEATURED_ENABLED_CARD_TYPE)).thenReturn(flowOf(data))

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, ACTIVITY_FEATURED_ENABLED_CARD_TYPE)
    }

    @Test
    fun `given activity feature enabled, when refresh is invoked, then activity are requested from network`() =
        test {
            init(isDashboardCardActivityLogEnabled = true)
            val result = mutableListOf<CardsUpdate>()
            whenever(cardsStore.getCards(siteModel, ACTIVITY_FEATURED_ENABLED_CARD_TYPE)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(siteModel, ACTIVITY_FEATURED_ENABLED_CARD_TYPE)).thenReturn(success)
            cardSource.refresh.observeForever { }

            cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
                it?.let { result.add(it) }
            }
            advanceUntilIdle()

            verify(cardsStore).fetchCards(siteModel, ACTIVITY_FEATURED_ENABLED_CARD_TYPE)
        }

    @Test
    fun `given activity feature disabled, when build is invoked, then activity not requested`() = test {
        init(isDashboardCardActivityLogEnabled = false)
        val result = mutableListOf<CardsUpdate>()

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        verify(cardsStore).getCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given activity feature disabled, when refresh is invoked, then activity not requested`() =
        test {
            init(isDashboardCardActivityLogEnabled = false)
            val result = mutableListOf<CardsUpdate>()
            whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
            whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(success).thenReturn(success)
            cardSource.refresh.observeForever { }

            cardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
                it?.let { result.add(it) }
            }
            advanceUntilIdle()

            verify(cardsStore).fetchCards(siteModel, DEFAULT_CARD_TYPE)
        }
}
