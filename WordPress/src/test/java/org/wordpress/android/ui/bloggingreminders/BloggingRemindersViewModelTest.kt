package org.wordpress.android.ui.bloggingreminders

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
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
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.toList
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider

class BloggingRemindersViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingRemindersManager: BloggingRemindersManager
    @Mock lateinit var bloggingRemindersStore: BloggingRemindersStore
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var daySelectionBuilder: DaySelectionBuilder
    private lateinit var viewModel: BloggingRemindersViewModel
    private val siteId = 123
    private lateinit var events: MutableList<Boolean>
    private lateinit var uiState: MutableList<List<BloggingRemindersItem>>

    @ExperimentalStdlibApi
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = BloggingRemindersViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                bloggingRemindersManager,
                bloggingRemindersStore,
                resourceProvider,
                daySelectionBuilder
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
    fun `date selection selected`() {
        val model = initEmptyStore()
        val daySelectionScreen = listOf<BloggingRemindersItem>()
        whenever(daySelectionBuilder.buildSelection(eq(model), any(), any())).thenReturn(daySelectionScreen)

        viewModel.showBottomSheet(siteId, SELECTION)

        assertThat(uiState.last()).isEqualTo(daySelectionScreen)
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

    @Test
    fun `switches from prologue do day selection on primary button click`() {
        viewModel.showBottomSheet(siteId, PROLOGUE)

        assertPrologue()

        clickPrimaryButton()

        assertThat(uiState.last()).isEqualTo(listOf<BloggingRemindersItem>())
    }

    @Test
    fun `switches from day selection do epilogue on primary button click`() {
        val model = BloggingRemindersModel(
                siteId,
                setOf(MONDAY)
        )
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(
                        model
                )
        )
        initDaySelectionBuilder()

        viewModel.showBottomSheet(siteId, SELECTION)

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

    private fun initEmptyStore(): BloggingRemindersModel {
        val emptyModel = BloggingRemindersModel(siteId)
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(flowOf(emptyModel))
        return emptyModel
    }

    private fun assertPrologue() {
        val state = uiState.last()
        assertIllustration(state[0], R.drawable.img_illustration_celebration_150dp)
        assertTitle(state[1], R.string.set_your_blogging_goals_title)
        assertHighEmphasisText(state[2], R.string.set_your_blogging_goals_message)
        assertPrimaryButton(state[3], R.string.set_your_blogging_goals_button, isEnabled = true)
    }

    private fun assertEpilogue() {
        val state = uiState.last()
        assertIllustration(state[0], R.drawable.img_illustration_bell_yellow_96dp)
        assertTitle(state[1], R.string.blogging_reminders_epilogue_title)
        assertTextWithParams(state[2], R.string.blogging_reminders_epilogue_body_days)
        assertCaption(state[3], R.string.blogging_reminders_epilogue_caption)
        assertPrimaryButton(state[4], R.string.blogging_reminders_done, isEnabled = true)
    }

    private fun assertIllustration(item: BloggingRemindersItem, @DrawableRes drawableRes: Int) {
        val illustration = item as Illustration
        assertThat(illustration.illustration).isEqualTo(drawableRes)
    }

    private fun assertTitle(item: BloggingRemindersItem, @StringRes titleRes: Int) {
        val title = item as Title
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(titleRes)
    }

    private fun assertHighEmphasisText(item: BloggingRemindersItem, @StringRes textRes: Int) {
        val title = item as HighEmphasisText
        assertThat((title.text as UiStringRes).stringRes).isEqualTo(textRes)
    }

    private fun assertTextWithParams(item: BloggingRemindersItem, @StringRes textRes: Int) {
        val title = item as MediumEmphasisText
        assertThat((title.text as UiStringResWithParams).stringRes).isEqualTo(textRes)
    }

    private fun assertCaption(item: BloggingRemindersItem, @StringRes textRes: Int) {
        val caption = item as Caption
        assertThat((caption.text as UiStringRes).stringRes).isEqualTo(textRes)
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

    private fun initDaySelectionBuilder() {
        doAnswer {
            val model = it.getArgument<BloggingRemindersModel>(0)
            val onDaySelected: (Day) -> Unit = it.getArgument(1)
            val onConfirm: (BloggingRemindersModel?) -> Unit = it.getArgument(2)
            listOf(
                    DayButtons(
                            Day.values()
                                    .map { day ->
                                        DayItem(
                                                UiStringText(day.name),
                                                model?.enabledDays?.contains(day) == true,
                                                Companion.create { onDaySelected.invoke(day) })
                                    }
                    ),
                    PrimaryButton(
                            UiStringText("Confirm"),
                            true,
                            ListItemInteraction.create { onConfirm.invoke(model) })
            )
        }.whenever(daySelectionBuilder).buildSelection(any(), any(), any())
    }
}
