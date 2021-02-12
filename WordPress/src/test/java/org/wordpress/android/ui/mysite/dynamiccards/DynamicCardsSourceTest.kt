package org.wordpress.android.ui.mysite.dynamiccards

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCards
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.prefs.AppPrefsWrapper

class DynamicCardsSourceTest : BaseUnitTest() {
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var dynamicCardsSource: DynamicCardsSource

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.getPinnedDynamicCardType()).thenReturn(null)
        dynamicCardsSource = DynamicCardsSource(appPrefsWrapper)
    }

    @Test
    fun `build all dynamic card types in original order`() = test {
        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
    }

    @Test
    fun `hides customize card`() = test {
        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }
        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)

        assertThat(result?.cards).containsExactly(GROW_QUICK_START)
    }

    @Test
    fun `hides grow card`() = test {
        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }
        dynamicCardsSource.hideItem(GROW_QUICK_START)

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `hides both cards`() = test {
        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }
        dynamicCardsSource.hideItem(GROW_QUICK_START)
        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)

        assertThat(result?.cards).isEmpty()
    }

    @Test
    fun `pin grow card reorders cards`() = test {
        val pinnedItem = GROW_QUICK_START
        whenever(appPrefsWrapper.getPinnedDynamicCardType()).thenReturn(null)

        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }

        dynamicCardsSource.pinItem(pinnedItem)

        assertThat(result?.cards).containsExactly(GROW_QUICK_START, CUSTOMIZE_QUICK_START)
        verify(appPrefsWrapper).pinDynamicCardType(pinnedItem)
    }

    @Test
    fun `repin originally first card`() = test {
        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }

        dynamicCardsSource.pinItem(GROW_QUICK_START)
        dynamicCardsSource.pinItem(CUSTOMIZE_QUICK_START)

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
        inOrder(appPrefsWrapper).apply {
            verify(appPrefsWrapper).pinDynamicCardType(GROW_QUICK_START)
            verify(appPrefsWrapper).pinDynamicCardType(CUSTOMIZE_QUICK_START)
        }
    }

    @Test
    fun `unpins card when already pinned`() = test {
        whenever(appPrefsWrapper.getPinnedDynamicCardType()).thenReturn(GROW_QUICK_START)
        val unpinnedItem = GROW_QUICK_START

        var result: DynamicCards? = null
        dynamicCardsSource.buildSource(this).observeForever { result = it }

        dynamicCardsSource.pinItem(unpinnedItem)

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
        verify(appPrefsWrapper).unpinDynamicCardType()
    }
}
