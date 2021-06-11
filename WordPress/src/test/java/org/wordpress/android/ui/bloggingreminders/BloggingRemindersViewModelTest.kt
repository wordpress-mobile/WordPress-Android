package org.wordpress.android.ui.bloggingreminders

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
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
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(emptyFlow())
    }

    @Test
    fun `sets blogging reminders as shown on PROLOGUE`() {
        viewModel.showBottomSheet(siteId, PROLOGUE)

        verify(bloggingRemindersManager).bloggingRemindersShown(siteId)
    }

    @Test
    fun `shows bottom sheet on showBottomSheet`() {
        viewModel.showBottomSheet(siteId, PROLOGUE)

        assertThat(events).containsExactly(true)
    }

    @Test
    fun `shows prologue ui state on PROLOGUE`() {
        viewModel.showBottomSheet(siteId, PROLOGUE)

        assertPrologue()
    }

    @Test
    fun `date selection disabled when model is empty`() {
        initEmptyStore()
        viewModel.showBottomSheet(siteId, SELECTION)

        assertDaySelection(primaryButtonEnabled = false)
    }

    @Test
    fun `date selection enabled button when model is not empty`() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(
                        BloggingRemindersModel(
                                siteId,
                                setOf(MONDAY)
                        )
                )
        )
        viewModel.showBottomSheet(siteId, SELECTION)

        assertDaySelection(primaryButtonEnabled = true)
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
                        eq(R.string.blogging_goals_n_times_a_week),
                        eq(UiStringText("2"))
                )
        ).thenReturn("Blogging reminders 2 times a week")
        var uiState: String? = null

        viewModel.getSettingsState(siteId).observeForever { uiState = it }

        assertThat(uiState).isEqualTo("Blogging reminders 2 times a week")
    }

    @Test
    fun `switches from prologue do day selection on primary button click`() {
        viewModel.showBottomSheet(siteId, PROLOGUE)

        assertPrologue()

        clickPrimaryButton()

        assertDaySelection(primaryButtonEnabled = false)
    }

    @Test
    fun `switches from day selection do epilogue on primary button click`() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(
                        BloggingRemindersModel(
                                siteId,
                                setOf(MONDAY)
                        )
                )
        )
        viewModel.showBottomSheet(siteId, SELECTION)

        assertDaySelection(primaryButtonEnabled = true)

        clickPrimaryButton()

        assertEpilogue()
    }

    @Test
    fun `closes bottom sheet from epilogue on primary button click`() {
        viewModel.showBottomSheet(siteId, EPILOGUE)

        assertEpilogue()

        assertThat(events.last()).isTrue()

        clickPrimaryButton()

        assertThat(events.last()).isFalse()
    }

    @Test
    fun `enables button on day selection`() {
        initEmptyStore()
        viewModel.showBottomSheet(siteId, SELECTION)

        assertDaySelection(primaryButtonEnabled = false)

        viewModel.selectDay(MONDAY)

        assertDaySelection(primaryButtonEnabled = true)
    }

    private fun initEmptyStore() {
        val emptyModel = BloggingRemindersModel(siteId)
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(flowOf(emptyModel))
    }

    private fun assertPrologue() {
        val state = uiState.last()
        assertIllustration(state[0], R.drawable.img_illustration_celebration_150dp)
        assertTitle(state[1], R.string.set_your_blogging_goals_title)
        assertText(state[2], R.string.set_your_blogging_goals_message)
        assertPrimaryButton(state[3], R.string.set_your_blogging_goals_button, isEnabled = true)
    }

    private fun assertDaySelection(primaryButtonEnabled: Boolean) {
        val state = uiState.last()
        // TODO change this method when the list contains the updated UI
        assertPrimaryButton(state[0], R.string.blogging_reminders_notify_me, isEnabled = primaryButtonEnabled)
    }

    private fun assertEpilogue() {
        val state = uiState.last()
        // TODO change this method when the list contains the updated UI
        assertPrimaryButton(state[0], R.string.blogging_reminders_done, isEnabled = true)
    }

    private fun assertIllustration(item: BloggingRemindersItem, @DrawableRes drawableRes: Int) {
        val illustration = item as Illustration
        assertThat(illustration.illustration).isEqualTo(drawableRes)
    }

    private fun assertTitle(item: BloggingRemindersItem, @StringRes titleRes: Int) {
        val title = item as Title
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(titleRes)
    }

    private fun assertText(item: BloggingRemindersItem, @StringRes textRes: Int) {
        val title = item as Text
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(textRes)
    }

    private fun assertPrimaryButton(
        item: BloggingRemindersItem,
        @StringRes buttonText: Int,
        isEnabled: Boolean = true
    ) {
        val primaryButton = item as PrimaryButton
        assertThat((primaryButton.text as UiStringRes).stringRes).isEqualTo(buttonText)
        assertThat(primaryButton.enabled).isEqualTo(isEnabled)
    }

    private fun clickPrimaryButton() {
        val primaryButton = uiState.last().find { it is PrimaryButton } as PrimaryButton
        primaryButton.onClick.click()
    }
}
