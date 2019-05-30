package org.wordpress.android.ui.stats.refresh.utils

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.StatsType

class NewsCardHandlerTest : BaseUnitTest() {
    @Mock private lateinit var statsStore: StatsStore
    private lateinit var newsCardHandler: NewsCardHandler
    @Before
    fun setUp() {
        newsCardHandler = NewsCardHandler(Dispatchers.Unconfined, statsStore)
    }

    @Test
    fun `dismiss hides news card and emits dismissed value when news card is showing`() {
        whenever(statsStore.isInsightsManagementNewsCardShowing()).thenReturn(true)
        var dismissedCard: StatsType? = null
        newsCardHandler.cardDismissed.observeForever { dismissedCard = it?.getContentIfNotHandled() }

        newsCardHandler.dismiss()

        verify(statsStore).hideInsightsManagementNewsCard()
        assertThat(dismissedCard).isEqualTo(ManagementType.NEWS_CARD)
    }

    @Test
    fun `dismiss does not do anything when news card is already hidden`() {
        whenever(statsStore.isInsightsManagementNewsCardShowing()).thenReturn(false)
        var dismissedCard: StatsType? = null
        newsCardHandler.cardDismissed.observeForever { dismissedCard = it?.getContentIfNotHandled() }

        newsCardHandler.dismiss()

        verify(statsStore, never()).hideInsightsManagementNewsCard()
        assertThat(dismissedCard).isNull()
    }

    @Test
    fun `edit scrolls to control card and hides toolbar`() {
        var scrollToCard: StatsType? = null
        newsCardHandler.scrollTo.observeForever { scrollToCard = it?.getContentIfNotHandled() }
        var toolbarHidden: Boolean? = null
        newsCardHandler.hideToolbar.observeForever { toolbarHidden = it?.getContentIfNotHandled() }

        newsCardHandler.goToEdit()

        assertThat(scrollToCard).isEqualTo(ManagementType.CONTROL)
        assertThat(toolbarHidden).isTrue()
    }
}
