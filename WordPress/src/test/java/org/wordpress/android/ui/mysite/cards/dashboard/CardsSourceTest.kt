package org.wordpress.android.ui.mysite.cards.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
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

private val CARDS_MODEL: List<CardModel> = listOf(
        TODAYS_STATS_CARDS_MODEL,
        POSTS_MODEL
)

private val DEFAULT_CARD_TYPE = listOf(CardModel.Type.POSTS)
private val STATS_FEATURED_ENABLED_CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class CardsSourceTest : BaseUnitTest() {
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var cardsStore: CardsStore
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var todaysStatsCardFeatureConfig: MySiteDashboardTodaysStatsCardFeatureConfig
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

    private fun init(isTodaysStatsCardFeatureConfigEnabled: Boolean = false) {
        setUpMocks(isTodaysStatsCardFeatureConfigEnabled)
        cardSource = CardsSource(
                selectedSiteRepository,
                cardsStore,
                todaysStatsCardFeatureConfig,
                TEST_DISPATCHER
        )
    }

    private fun setUpMocks(isTodaysStatsCardFeatureConfigEnabled: Boolean) {
        whenever(todaysStatsCardFeatureConfig.isEnabled()).thenReturn(isTodaysStatsCardFeatureConfigEnabled)
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
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

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        verify(cardsStore).getCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)
    }

    @Test
    fun `given build is invoked, when cards are collected, then data is loaded (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data.model))
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then cards are fetched from store (network)`() = test {
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(CardsResult(CARDS_MODEL)))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(cardsStore).fetchCards(siteModel, DEFAULT_CARD_TYPE)
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get cards (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(CardsResult())
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

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

                cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

                verify(cardsStore).fetchCards(siteModel, STATS_FEATURED_ENABLED_CARD_TYPES)
            }

    @Test
    fun `given error, when build is invoked, then error snackbar with stale message is also shown (network)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel, DEFAULT_CARD_TYPE)).thenReturn(apiError)
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(
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
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

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
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        cardSource.refresh()

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

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(result[3]).isTrue // refresh()
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

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> cardsStore.fetchCards(...) -> success
        assertThat(result[3]).isTrue // refresh()
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

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> cardsStore.fetchCards(...) -> error
        assertThat(result[3]).isTrue // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> cardsStore.fetchCards(...) -> error
    }

    /* INVALID SITE */

    @Test
    fun `given invalid site, when build is invoked, then error card is shown`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), invalidSiteLocalId).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(showErrorCard = true))
    }

    @Test
    fun `given invalid site, when refresh is invoked, then error card is shown`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.refresh.observeForever { }
        cardSource.build(testScope(), invalidSiteLocalId).observeForever { result.add(it) }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(CardsUpdate(showErrorCard = true))
        assertThat(result.last()).isEqualTo(CardsUpdate(showErrorCard = true))
    }
}
