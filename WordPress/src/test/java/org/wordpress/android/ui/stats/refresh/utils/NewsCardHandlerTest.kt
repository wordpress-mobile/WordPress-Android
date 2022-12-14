package org.wordpress.android.ui.stats.refresh.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.StatsType

@ExperimentalCoroutinesApi
class NewsCardHandlerTest : BaseUnitTest() {
    @Mock private lateinit var statsStore: StatsStore
    private lateinit var newsCardHandler: NewsCardHandler

    @Before
    fun setUp() {
        newsCardHandler = NewsCardHandler(
                testDispatcher(),
                statsStore
        )
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
