package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GET_TO_KNOW_APP_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.persistence.DynamicCardSqlUtils
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class DynamicCardStoreTest {
    @Mock lateinit var dynamicCardSqlUtils: DynamicCardSqlUtils
    private lateinit var dynamicCardStore: DynamicCardStore
    private val siteId = 1
    private val dynamicCardTypes = listOf(
        CUSTOMIZE_QUICK_START,
        GROW_QUICK_START,
        GET_TO_KNOW_APP_QUICK_START
    )

    @Before
    fun setUp() {
        dynamicCardStore = DynamicCardStore(
                initCoroutineEngine(),
                dynamicCardSqlUtils
        )
    }

    @Test
    fun `pin calls sql utils`() = test {
        dynamicCardStore.pinCard(siteId, CUSTOMIZE_QUICK_START)

        verify(dynamicCardSqlUtils).pin(siteId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `unpin calls sql utils`() = test {
        dynamicCardStore.unpinCard(siteId)

        verify(dynamicCardSqlUtils).unpin(siteId)
    }

    @Test
    fun `remove calls sql utils`() = test {
        dynamicCardStore.removeCard(siteId, CUSTOMIZE_QUICK_START)

        verify(dynamicCardSqlUtils).remove(siteId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `returns all items when no hidden or removed cards`() = test {
        initEmptyDatabase()

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).isEqualTo(dynamicCardTypes)
    }

    @Test
    fun `hides customize card`() = test {
        initEmptyDatabase()
        dynamicCardStore.hideCard(siteId, CUSTOMIZE_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).doesNotContain(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `hides grow card`() = test {
        initEmptyDatabase()
        dynamicCardStore.hideCard(siteId, GROW_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).doesNotContain(GROW_QUICK_START)
    }

    @Test
    fun `does not return customize card from when it's removed`() = test {
        initDatabase(removedCards = listOf(CUSTOMIZE_QUICK_START))

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).doesNotContain(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `does not return grow card from when it's removed`() = test {
        initDatabase(removedCards = listOf(GROW_QUICK_START))

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).doesNotContain(GROW_QUICK_START)
    }

    @Test
    fun `does not return any cards when all are removed`() = test {
        initDatabase(removedCards = dynamicCardTypes)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isNull()
        assertThat(result.dynamicCardTypes).isEmpty()
    }

    @Test
    fun `returns pinned grow card in the first place`() = test {
        initDatabase(pinnedItem = GROW_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isEqualTo(GROW_QUICK_START)
        assertThat(result.dynamicCardTypes)
            .containsExactly(GROW_QUICK_START, CUSTOMIZE_QUICK_START, GET_TO_KNOW_APP_QUICK_START)
    }

    @Test
    fun `returns pinned customize card in the first place`() = test {
        initDatabase(pinnedItem = CUSTOMIZE_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result.pinnedItem).isEqualTo(CUSTOMIZE_QUICK_START)
        assertThat(result.dynamicCardTypes).isEqualTo(dynamicCardTypes)
    }

    private fun initEmptyDatabase() {
        initDatabase()
    }

    private fun initDatabase(pinnedItem: DynamicCardType? = null, removedCards: List<DynamicCardType> = listOf()) {
        whenever(dynamicCardSqlUtils.selectPinned(any())).thenReturn(pinnedItem)
        whenever(dynamicCardSqlUtils.selectRemoved(any())).thenReturn(removedCards)
    }
}
