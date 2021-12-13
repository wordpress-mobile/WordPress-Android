package org.wordpress.android.ui.mysite.dynamiccards

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardsModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository

class DynamicCardsSourceTest : BaseUnitTest() {
    @Mock lateinit var dynamicCardStore: DynamicCardStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var siteModel: SiteModel
    private lateinit var dynamicCardsSource: DynamicCardsSource
    private val siteLocalId: Int = 1
    private lateinit var isRefreshing: MutableList<Boolean>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        dynamicCardsSource = DynamicCardsSource(dynamicCardStore, selectedSiteRepository)
        whenever(siteModel.id).thenReturn(siteLocalId)
        isRefreshing = mutableListOf()
    }

    @Test
    fun `returns cards from the store`() = test {
        val pinnedItem = GROW_QUICK_START
        val dynamicCardTypes = listOf(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
        whenever(dynamicCardStore.getCards(siteLocalId)).thenReturn(
                DynamicCardsModel(
                        pinnedItem,
                        dynamicCardTypes
                )
        )
        var result: DynamicCardsUpdate? = null
        dynamicCardsSource.build(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result?.pinnedDynamicCard).isEqualTo(pinnedItem)
        assertThat(result?.cards).isEqualTo(dynamicCardTypes)
    }

    @Test
    fun `hides card when site is present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)

        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)

        verify(dynamicCardStore).hideCard(siteLocalId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `does not hide card when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        dynamicCardsSource.hideItem(GROW_QUICK_START)

        verifyZeroInteractions(dynamicCardStore)
    }

    @Test
    fun `pins card when site is present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)

        dynamicCardsSource.pinItem(CUSTOMIZE_QUICK_START)

        verify(dynamicCardStore).pinCard(siteLocalId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `does not pin card when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        dynamicCardsSource.pinItem(GROW_QUICK_START)

        verifyZeroInteractions(dynamicCardStore)
    }

    @Test
    fun `unpins when site is present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)

        dynamicCardsSource.unpinItem()

        verify(dynamicCardStore).unpinCard(siteLocalId)
    }

    @Test
    fun `does not unpin when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        dynamicCardsSource.unpinItem()

        verifyZeroInteractions(dynamicCardStore)
    }

    @Test
    fun `when source is invoked, then refresh is false`() = test {
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.build(testScope(), siteLocalId)

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        val pinnedItem = GROW_QUICK_START
        val dynamicCardTypes = listOf(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
        whenever(dynamicCardStore.getCards(siteLocalId)).thenReturn(
                DynamicCardsModel(
                        pinnedItem,
                        dynamicCardTypes
                )
        )
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.build(testScope(), siteLocalId).observeForever { }
        dynamicCardsSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }
}
