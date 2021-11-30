package org.wordpress.android.ui.mysite.cards.dashboard

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository

/* SITE */

const val SITE_LOCAL_ID = 1

class CardsSourceTest : BaseUnitTest() {
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var cardsStore: CardsStore
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var cardSource: CardsSource
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        cardSource = CardsSource(selectedSiteRepository, cardsStore)
        isRefreshing = mutableListOf()
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
    }

    /* DATA */

    @Test
    fun `when source is requested upon start, then data is loaded`() = test {
        var result: CardsUpdate? = null
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(CardsResult())

        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result = it } }

        assertThat(result?.cards).isNotNull
    }

    @Test
    fun `when refresh is invoked, then data is refreshed`() = test {
        val result: MutableList<CardsUpdate?> = mutableListOf()
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(CardsResult())
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }
        cardSource.refresh.observeForever { isRefreshing.add(it) }

        cardSource.refresh()

        assertThat(result.first()?.cards).isNotNull
        assertThat(result.last()?.cards).isNotNull
        assertThat(result.size).isEqualTo(2)
    }

    /* IS REFRESHING */

    @Test
    fun `when source is invoked, then refresh is false`() = test {
        cardSource.refresh.observeForever { isRefreshing.add(it) }

        cardSource.build(testScope(), SITE_LOCAL_ID)

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        cardSource.refresh.observeForever { isRefreshing.add(it) }

        cardSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        val result: MutableList<CardsUpdate?> = mutableListOf()
        whenever(cardsStore.fetchCards(siteModel)).thenReturn(CardsResult())
        cardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }
        cardSource.refresh.observeForever { isRefreshing.add(it) }

        cardSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }
}
