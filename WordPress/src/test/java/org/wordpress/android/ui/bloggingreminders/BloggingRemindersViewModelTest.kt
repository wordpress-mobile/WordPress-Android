package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.eventToList
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.toList
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.ResourceProvider

class BloggingRemindersViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingRemindersManager: BloggingRemindersManager
    @Mock lateinit var bloggingRemindersStore: BloggingRemindersStore
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: BloggingRemindersViewModel
    private val siteId = 123
    private lateinit var events: MutableList<Boolean>
    private lateinit var uiState: MutableList<List<BloggingRemindersItem>>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = BloggingRemindersViewModel(
                bloggingRemindersManager,
                bloggingRemindersStore,
                resourceProvider,
                TEST_DISPATCHER
        )
        events = mutableListOf()
        events = viewModel.isBottomSheetShowing.eventToList()
        uiState = viewModel.uiState.toList()
    }

    @Test
    fun `sets blogging reminders as shown on showBottomSheet`() {
        viewModel.showBottomSheet(siteId)

        verify(bloggingRemindersManager).bloggingRemindersShown(siteId)
    }

    @Test
    fun `shows bottom sheet on showBottomSheet`() {
        viewModel.showBottomSheet(siteId)

        assertThat(events).containsExactly(true)
    }

    @Test
    fun `shows ui state on showBottomSheet`() {
        viewModel.showBottomSheet(siteId)

        val state = uiState.last()

        assertIllustration(state[0])
        assertTitle(state[1])
        assertText(state[2])
        assertPrimaryButton(state[3])
    }

    @Test
    fun `inits blogging reminders state`() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(
                        BloggingRemindersModel(
                                siteId,
                                setOf(MONDAY, SUNDAY)
                        )
                )
        )
        whenever(
                resourceProvider.getString(
                        R.string.blogging_goals_n_times_a_week,
                        2
                )
        ).thenReturn("Blogging reminders 2 times a week")
        var uiState: String? = null

        viewModel.getSettingsState(siteId).observeForever { uiState = it }

        assertThat(uiState).isEqualTo("Blogging reminders 2 times a week")
    }

    private fun assertIllustration(item: BloggingRemindersItem) {
        val illustration = item as Illustration
        assertThat(illustration.illustration).isEqualTo(R.drawable.img_illustration_celebration_150dp)
    }

    private fun assertTitle(item: BloggingRemindersItem) {
        val title = item as Title
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(R.string.set_your_blogging_goals_title)
    }

    private fun assertText(item: BloggingRemindersItem) {
        val title = item as Text
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(R.string.set_your_blogging_goals_message)
    }

    private fun assertPrimaryButton(item: BloggingRemindersItem) {
        val closeButton = item as PrimaryButton
        assertThat((closeButton.text as UiStringRes).stringRes).isEqualTo(R.string.set_your_blogging_goals_button)
    }
}
