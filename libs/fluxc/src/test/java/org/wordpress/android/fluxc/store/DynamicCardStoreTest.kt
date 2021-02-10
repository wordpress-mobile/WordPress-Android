package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.persistence.DynamicCardSqlUtils
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class DynamicCardStoreTest {
    @Mock lateinit var dynamicCardSqlUtils: DynamicCardSqlUtils
    private lateinit var dynamicCardStore: DynamicCardStore
    private val siteId = 1
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
    fun `remove calls sql utils`() = test {
        dynamicCardStore.removeCard(siteId, CUSTOMIZE_QUICK_START)

        verify(dynamicCardSqlUtils).remove(siteId, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `returns all items when no hidden or removed cards`() = test {
        initEmptyDatabase()

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
    }

    @Test
    fun `hides customize card`() = test {
        initEmptyDatabase()
        dynamicCardStore.hideCard(siteId, CUSTOMIZE_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(GROW_QUICK_START)
    }

    @Test
    fun `hides grow card`() = test {
        initEmptyDatabase()
        dynamicCardStore.hideCard(siteId, GROW_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `does not return customize card from when it's removed`() = test {
        initDatabase(removedCards = listOf(CUSTOMIZE_QUICK_START))

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(GROW_QUICK_START)
    }

    @Test
    fun `does not return grow card from when it's removed`() = test {
        initDatabase(removedCards = listOf(GROW_QUICK_START))

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `does not return any cards when all are removed`() = test {
        initDatabase(removedCards = listOf(CUSTOMIZE_QUICK_START, GROW_QUICK_START))

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `returns pinned grow card in the first place`() = test {
        initDatabase(pinnedItem = GROW_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(GROW_QUICK_START, CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `returns pinned customize card in the first place`() = test {
        initDatabase(pinnedItem = CUSTOMIZE_QUICK_START)

        val result = dynamicCardStore.getCards(siteId)

        assertThat(result).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
    }

    private fun initEmptyDatabase() {
        initDatabase()
    }

    private fun initDatabase(pinnedItem: DynamicCardType? = null, removedCards: List<DynamicCardType> = listOf()) {
        whenever(dynamicCardSqlUtils.selectPinned(any())).thenReturn(pinnedItem)
        whenever(dynamicCardSqlUtils.selectRemoved(any())).thenReturn(removedCards)
    }
}
