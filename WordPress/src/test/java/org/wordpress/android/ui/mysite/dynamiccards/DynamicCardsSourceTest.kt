package org.wordpress.android.ui.mysite.dynamiccards

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
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
        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
    }

    @Test
    fun `hides customize card`() = test {
        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)
        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).containsExactly(GROW_QUICK_START)
    }

    @Test
    fun `hides grow card`() = test {
        dynamicCardsSource.hideItem(GROW_QUICK_START)
        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START)
    }

    @Test
    fun `hides both cards`() = test {
        dynamicCardsSource.hideItem(GROW_QUICK_START)
        dynamicCardsSource.hideItem(CUSTOMIZE_QUICK_START)
        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).isEmpty()
    }

    @Test
    fun `pin grow card reorders cards`() = test {
        val pinnedItem = GROW_QUICK_START
        whenever(appPrefsWrapper.getPinnedDynamicCardType()).thenReturn(null)
        dynamicCardsSource.pinItem(pinnedItem)

        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).containsExactly(GROW_QUICK_START, CUSTOMIZE_QUICK_START)
        verify(appPrefsWrapper).pinDynamicCardType(pinnedItem)
    }

    @Test
    fun `repin originally first card`() = test {
        dynamicCardsSource.pinItem(GROW_QUICK_START)
        dynamicCardsSource.pinItem(CUSTOMIZE_QUICK_START)

        val result = dynamicCardsSource.buildSource().take(1).toList().last()

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
        dynamicCardsSource.pinItem(unpinnedItem)

        val result = dynamicCardsSource.buildSource().take(1).toList().last()

        assertThat(result?.cards).containsExactly(CUSTOMIZE_QUICK_START, GROW_QUICK_START)
        verify(appPrefsWrapper).unpinDynamicCardType()
    }
}
