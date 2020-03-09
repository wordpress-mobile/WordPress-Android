package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BigTitle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.DialogButtons
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.utils.NewsCardHandler
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

class ManagementNewsCardUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var newsCardHandler: NewsCardHandler
    @Mock private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var useCase: ManagementNewsCardUseCase
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = ManagementNewsCardUseCase(
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                resourceProvider,
                newsCardHandler,
                analyticsTrackerWrapper
        )
    }

    @Test
    fun `builds a card`() = test {
        val editTextButton = "Edit"
        val newsCardMessage = "You can $editTextButton your insights"
        whenever(resourceProvider.getString(R.string.stats_management_add_new_stats_card)).thenReturn(
                editTextButton
        )
        whenever(
                resourceProvider.getString(
                        R.string.stats_management_news_card_message,
                        editTextButton
                )
        ).thenReturn(newsCardMessage)
        val result = loadData()

        val data = result.data!!
        assertThat(data).hasSize(5)

        assertThat((data[0] as ImageItem).imageResource).isEqualTo(R.drawable.insights_management_feature_resource)
        assertThat((data[1] as Tag).textResource).isEqualTo(R.string.stats_management_new)
        assertThat((data[2] as BigTitle).textResource).isEqualTo(R.string.stats_manage_your_stats)
        val messageItem = data[3] as Text
        assertThat(messageItem.text).isEqualTo(newsCardMessage)
        assertThat(messageItem.bolds).isEqualTo(listOf(editTextButton))
        val dialogButtons = data[4] as DialogButtons
        assertThat(dialogButtons.positiveButtonText).isEqualTo(R.string.stats_management_try_it_now)
        assertThat(dialogButtons.negativeButtonText).isEqualTo(R.string.stats_management_dismiss_insights_news_card)

        dialogButtons.positiveAction.click()

        verify(analyticsTrackerWrapper).track(Stat.STATS_INSIGHTS_MANAGEMENT_HINT_CLICKED)
        verify(newsCardHandler).goToEdit()

        dialogButtons.negativeAction.click()

        verify(analyticsTrackerWrapper).track(Stat.STATS_INSIGHTS_MANAGEMENT_HINT_DISMISSED)
        verify(newsCardHandler).dismiss()
    }

    private suspend fun loadData(): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh = false, forced = false)
        return checkNotNull(result)
    }
}
