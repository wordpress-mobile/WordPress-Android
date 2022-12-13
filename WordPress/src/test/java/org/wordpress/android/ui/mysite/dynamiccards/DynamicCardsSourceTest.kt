package org.wordpress.android.ui.mysite.dynamiccards

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardsModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@ExperimentalCoroutinesApi
class DynamicCardsSourceTest : BaseUnitTest() {
    @Mock lateinit var dynamicCardStore: DynamicCardStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var siteModel: SiteModel
    private lateinit var dynamicCardsSource: DynamicCardsSource
    private val siteLocalId: Int = 1
    private lateinit var isRefreshing: MutableList<Boolean>
    private val pinnedItem = GROW_QUICK_START
    private val dynamicCardTypes = listOf(CUSTOMIZE_QUICK_START, GROW_QUICK_START)

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        isRefreshing = mutableListOf()
    }

    @Test
    fun `given selected site, when source is build, then cards returned from the store`() = test {
        initDynamicCardsSource(hasSelectedSite = true)

        var result: DynamicCardsUpdate? = null
        dynamicCardsSource.build(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result?.pinnedDynamicCard).isEqualTo(pinnedItem)
        assertThat(result?.cards).isEqualTo(dynamicCardTypes)
    }

    @Test
    fun `given selected site, when hide action done, then card hidden`() = test {
        initDynamicCardsSource(hasSelectedSite = true)

        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)

        verify(dynamicCardStore).hideCard(siteLocalId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `given no selected site, when hide action done, then card not hidden`() = test {
        initDynamicCardsSource(hasSelectedSite = false)

        dynamicCardsSource.hideItem(GROW_QUICK_START)

        verifyNoInteractions(dynamicCardStore)
    }

    @Test
    fun `given selected site, when pin action done, then card is pinned`() = test {
        initDynamicCardsSource(hasSelectedSite = true)

        dynamicCardsSource.pinItem(CUSTOMIZE_QUICK_START)

        verify(dynamicCardStore).pinCard(siteLocalId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `given no selected site, when pin action done, then card is not pinned`() = test {
        initDynamicCardsSource(hasSelectedSite = false)

        dynamicCardsSource.pinItem(GROW_QUICK_START)

        verifyNoInteractions(dynamicCardStore)
    }

    @Test
    fun `given selected site, when unpin action done, then card is unpinned`() = test {
        initDynamicCardsSource(hasSelectedSite = true)

        dynamicCardsSource.unpinItem()

        verify(dynamicCardStore).unpinCard(siteLocalId)
    }

    @Test
    fun `given no selected site, when unpin action done, then card is not unpinned`() = test {
        initDynamicCardsSource(hasSelectedSite = false)

        dynamicCardsSource.unpinItem()

        verifyNoInteractions(dynamicCardStore)
    }

    @Test
    fun `given selected site, when source is build, then refresh is true`() = test {
        initDynamicCardsSource(hasSelectedSite = true)
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.build(testScope(), siteLocalId)

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `given selected site, when refresh is invoked, then refresh is true`() = test {
        initDynamicCardsSource(hasSelectedSite = true)
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `given selected site, when data has been refreshed, then refresh is set to false`() = test {
        initDynamicCardsSource(hasSelectedSite = true)
        dynamicCardsSource.refresh.observeForever { isRefreshing.add(it) }

        dynamicCardsSource.build(testScope(), siteLocalId).observeForever { }
        dynamicCardsSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    private suspend fun initDynamicCardsSource(
        hasSelectedSite: Boolean = true
    ) {
        whenever(siteModel.id).thenReturn(siteLocalId)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(if (hasSelectedSite) siteModel else null)
        if (hasSelectedSite) {
            val pinnedItem = GROW_QUICK_START
            val dynamicCardTypes = listOf(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
            whenever(dynamicCardStore.getCards(siteLocalId)).thenReturn(
                    DynamicCardsModel(
                            pinnedItem,
                            dynamicCardTypes
                    )
            )
        }
        dynamicCardsSource = DynamicCardsSource(dynamicCardStore, selectedSiteRepository)
    }
}
