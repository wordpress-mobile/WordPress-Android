package org.wordpress.android.ui.mysite.cards.dashboard

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository

/* SITE */

const val SITE_LOCAL_ID = 1

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
const val POST_FEATURED_IMAGE = "featuredImage"
const val POST_DATE = "2021-12-27 11:33:55"

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

private val CARDS_MODEL: List<CardModel> = listOf(
        POSTS_MODEL
)

@InternalCoroutinesApi
class CardsSourceTest : BaseUnitTest() {
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var cardsStore: CardsStore
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var cardSource: CardsSource

    private val data = CardsResult(
            model = CARDS_MODEL
    )
    private val success = CardsResult<List<CardModel>>()
    private val apiError = CardsResult<List<CardModel>>(
            error = CardsError(CardsErrorType.API_ERROR)
    )
    private val genericError = CardsResult<List<CardModel>>(
            error = CardsError(CardsErrorType.GENERIC_ERROR)
    )

    @Before
    fun setUp() {
        cardSource = CardsSource(
                selectedSiteRepository,
                cardsStore,
                TEST_DISPATCHER
        )
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
    }

    /* GET DATA */

    @Test
    fun `when build is invoked, then start collecting cards from store (database)`() = test {
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(cardsStore).getCards(siteModel)
    }

    @Test
    fun `given build is invoked, when cards are collected, then data is loaded (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data))
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then cards are fetched from store (network)`() = test {
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(CardsResult(CARDS_MODEL)))
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(cardsStore).fetchCards(siteModel)
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get cards (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(CardsResult())
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data))
    }

    @Test
    fun `given error, when build is invoked, then error data is also loaded (network)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(apiError)
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(apiError))
    }

    @Test
    fun `given no error, when refresh is invoked, then data is only loaded from get cards (database)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(success).thenReturn(success)
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }
        cardSource.refresh.observeForever { }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(data))
    }

    @Test
    fun `given error, when refresh is invoked, then error data is also loaded (network)`() = test {
        val result = mutableListOf<CardsUpdate>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(success).thenReturn(apiError)
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }
        cardSource.refresh.observeForever { }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(CardsUpdate(data))
        assertThat(result.last()).isEqualTo(CardsUpdate(apiError))
    }

    /* IS REFRESHING */

    @Test
    fun `when build is invoked, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        cardSource.refresh.observeForever { result.add(it) }

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.last()).isFalse
    }

    @Test
    fun `when refresh is invoked, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        cardSource.refresh.observeForever { result.add(it) }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isTrue
    }

    @Test
    fun `given no error, when data has been refreshed, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(success)
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        cardSource.refresh.observeForever { result.add(it) }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isTrue
    }

    @Test
    fun `given error, when data has been refreshed, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(cardsStore.getCards(siteModel)).thenReturn(flowOf(data))
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(apiError)
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        cardSource.refresh.observeForever { result.add(it) }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isFalse
    }

    /* INVALID SITE */

    @Test
    fun `given invalid site, when build is invoked, then error data is loaded`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.refresh.observeForever { }

        cardSource.build(testScope(), invalidSiteLocalId).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(CardsUpdate(genericError))
    }

    @Test
    fun `given invalid site, when refresh is invoked, then error data is loaded`() = test {
        val invalidSiteLocalId = 2
        val result = mutableListOf<CardsUpdate>()
        cardSource.build(testScope(), invalidSiteLocalId).observeForever { result.add(it) }
        cardSource.refresh.observeForever { }

        cardSource.refresh()

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(CardsUpdate(genericError))
        assertThat(result.last()).isEqualTo(CardsUpdate(genericError))
    }
}
