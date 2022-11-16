package org.wordpress.android.ui.bloggingreminders

import androidx.annotation.StringRes
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.eventToList
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.FRIDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.test
import org.wordpress.android.toList
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.BLOG_SETTINGS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.PUBLISH_FLOW
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import org.wordpress.android.workers.reminder.ReminderScheduler
import java.time.DayOfWeek

class BloggingRemindersViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingRemindersManager: BloggingRemindersManager
    @Mock lateinit var bloggingRemindersStore: BloggingRemindersStore
    @Mock lateinit var prologueBuilder: PrologueBuilder
    @Mock lateinit var epilogueBuilder: EpilogueBuilder
    @Mock lateinit var daySelectionBuilder: DaySelectionBuilder
    @Mock lateinit var dayLabelUtils: DayLabelUtils
    @Mock lateinit var analyticsTracker: BloggingRemindersAnalyticsTracker
    @Mock lateinit var reminderScheduler: ReminderScheduler
    @Mock lateinit var siteStore: SiteStore
    private lateinit var viewModel: BloggingRemindersViewModel
    private val siteId = 123
    private val hour = 10
    private val minute = 0
    private lateinit var events: MutableList<Boolean>
    private lateinit var uiState: MutableList<UiState>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = BloggingRemindersViewModel(
                TEST_DISPATCHER,
                bloggingRemindersManager,
                bloggingRemindersStore,
                prologueBuilder,
                daySelectionBuilder,
                epilogueBuilder,
                dayLabelUtils,
                analyticsTracker,
                reminderScheduler,
                BloggingRemindersModelMapper(),
                siteStore
        )
        events = mutableListOf()
        events = viewModel.isBottomSheetShowing.eventToList()
        uiState = viewModel.uiState.toList()
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(emptyFlow())
    }

    @Test
    fun `sets blogging reminders as shown on PROLOGUE`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)

        verify(bloggingRemindersManager).bloggingRemindersShown(siteId)
    }

    @Test
    fun `sets blogging reminders as shown from SiteSettings when has no previous blogging reminders`() = test {
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(false)

        viewModel.onBlogSettingsItemClicked(siteId)

        verify(bloggingRemindersManager).bloggingRemindersShown(siteId)
    }

    @Test
    fun `shows bottom sheet on showBottomSheet`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)

        viewModel.onPublishingPost(siteId, true)

        assertThat(events).containsExactly(true)
    }

    @Test
    fun `shows bottom sheet on onBloggingPromptSchedulingRequested`() {
        viewModel.onBloggingPromptSchedulingRequested(siteId)

        assertThat(events).containsExactly(true)
    }

    @Test
    fun `shows prologue ui state on PROLOGUE`() {
        val uiItems = initPrologueBuilder()
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)

        viewModel.onPublishingPost(siteId, true)

        assertThat(uiState.last().uiItems).isEqualTo(uiItems)
    }

    @Test
    fun `shows prologue ui state on PROLOGUE from SiteSettings`() = test {
        val uiItems = initPrologueBuilderForSiteSettings()
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(false)

        viewModel.onBlogSettingsItemClicked(siteId)

        assertThat(uiState.last().uiItems).isEqualTo(uiItems)
    }

    @Test
    fun `date selection selected`() = test {
        val model = initEmptyStore()
        val daySelectionScreen = listOf<BloggingRemindersItem>()
        whenever(daySelectionBuilder.buildSelection(eq(model), any(), any(), any(), any())).thenReturn(
                daySelectionScreen
        )
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        viewModel.onBlogSettingsItemClicked(siteId)

        assertThat(uiState.last().uiItems).isEqualTo(daySelectionScreen)
    }

    @Test
    fun `inits blogging reminders state`() {
        val model = BloggingRemindersModel(
                siteId,
                setOf(MONDAY, SUNDAY)
        )
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(
                        model
                )
        )
        val dayLabel = UiStringText("Blogging reminders 2 times a week at 10:00 am")
        whenever(
                dayLabelUtils.buildSiteSettingsLabel(
                        BloggingRemindersUiModel(
                                siteId,
                                setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY),
                                hour,
                                minute,
                                false
                        )
                )
        ).thenReturn(dayLabel)
        var uiState: UiString? = null

        viewModel.getBlogSettingsUiState(siteId).observeForever { uiState = it }

        assertThat(uiState).isEqualTo(dayLabel)
    }

    @Test
    fun `switches from prologue do day selection on primary button click`() {
        initPrologueBuilder()
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)

        clickPrimaryButton()

        assertThat(uiState.last().uiItems).isEqualTo(listOf<BloggingRemindersItem>())
    }

    @Test
    fun `switches from day selection do epilogue on primary button click`() = test {
        openEpilogue()

        assertEpilogue()
    }

    private suspend fun openEpilogue() {
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
        initEpilogueBuilder()
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        viewModel.onBlogSettingsItemClicked(siteId)

        clickPrimaryButton()
    }

    @Test
    fun `closes bottom sheet from epilogue on primary button click`() = test {
        openEpilogue()

        assertEpilogue()

        assertThat(events.last()).isTrue()

        clickPrimaryButton()

        assertThat(events.last()).isFalse()
    }

    @Test
    fun `showBottomSheet sets tracker site id`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)

        viewModel.onPublishingPost(siteId, true)

        verify(analyticsTracker).setSite(siteId)
    }

    @Test
    fun `showBottomSheet tracks flow start with correct source`() = test {
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)
        viewModel.onBlogSettingsItemClicked(siteId)
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)

        verify(analyticsTracker).trackFlowStart(BLOG_SETTINGS)
        verify(analyticsTracker).trackFlowStart(PUBLISH_FLOW)
    }

    @Test
    fun `showBottomSheet tracks screen shown with correct screen`() = test {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)
        viewModel.onBlogSettingsItemClicked(siteId)

        verify(analyticsTracker).trackScreenShown(PROLOGUE)
        verify(analyticsTracker).trackScreenShown(SELECTION)
    }

    @Test
    fun `showBottomSheet tracks screen shown more than once`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)
        viewModel.onPublishingPost(siteId, true)

        verify(analyticsTracker, times(2)).trackScreenShown(PROLOGUE)
    }

    @Test
    fun `clicking primary button on prologue screen tracks correct events`() {
        initPrologueBuilder()
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)

        clickPrimaryButton()

        verify(analyticsTracker).trackPrimaryButtonPressed(PROLOGUE)
        verify(analyticsTracker).trackScreenShown(SELECTION)
    }

    @Test
    fun `clicking primary button on selection screen tracks correct events`() = test {
        initEmptyStore()
        initDaySelectionBuilder()
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        viewModel.onBlogSettingsItemClicked(siteId)

        clickPrimaryButton()

        verify(analyticsTracker).trackPrimaryButtonPressed(SELECTION)
        verify(analyticsTracker).trackScreenShown(EPILOGUE)
    }

    @Test
    fun `clicking primary button on epilogue screen tracks correct events`() = test {
        openEpilogue()

        clickPrimaryButton()

        verify(analyticsTracker).trackPrimaryButtonPressed(EPILOGUE)
    }

    @Test
    fun `dismissing bottom sheet on prologue screen tracks dismiss event`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)
        viewModel.onPublishingPost(siteId, true)
        viewModel.onBottomSheetDismissed()

        verify(analyticsTracker).trackFlowDismissed(PROLOGUE)
    }

    @Test
    fun `dismissing bottom sheet on selection screen tracks dismiss event`() = test {
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)
        viewModel.onBlogSettingsItemClicked(siteId)
        viewModel.onBottomSheetDismissed()

        verify(analyticsTracker).trackFlowDismissed(SELECTION)
    }

    @Test
    fun `dismissing bottom sheet on epilogue screen tracks completed event`() = test {
        openEpilogue()
        viewModel.onBottomSheetDismissed()

        verify(analyticsTracker).trackFlowCompleted()
    }

    @Test
    fun `clicking primary button on selection screen schedule reminders with correct days`() = test {
        val model = BloggingRemindersModel(siteId, setOf(MONDAY, WEDNESDAY, FRIDAY))
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(flowOf(model))
        initDaySelectionBuilder()
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        viewModel.onBlogSettingsItemClicked(siteId)

        clickPrimaryButton()

        verify(reminderScheduler).schedule(
                siteId,
                hour,
                minute,
                WeeklyReminder(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        )
    }

    @Test
    fun `clicking primary button on empty selection screen cancel reminders`() = test {
        initEmptyStore()
        initDaySelectionBuilder()
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        viewModel.onBlogSettingsItemClicked(siteId)

        clickPrimaryButton()

        verify(reminderScheduler).cancelBySiteId(siteId)
    }

    @Test
    fun `onPublishingPost shows prologue when publishing for the first time and prompt was not shown before`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(true)

        initPrologueBuilder()

        viewModel.onPublishingPost(siteId, true)

        assertPrologue()
    }

    @Test
    fun `onPublishingPost does not show prologue when post was already published and prompt was not shown before`() {
        initPrologueBuilder()

        viewModel.onPublishingPost(siteId, false)

        assertThat(uiState.last().uiItems).isEqualTo(emptyList<BloggingRemindersItem>())
    }

    @Test
    fun `onPublishingPost does not show prologue when publishing for the first time and prompt was shown before`() {
        whenever(bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)).thenReturn(false)

        initPrologueBuilder()

        viewModel.onPublishingPost(siteId, true)

        assertThat(uiState.last().uiItems).isEqualTo(emptyList<BloggingRemindersItem>())
    }

    @Test
    fun `onSettingsItemClicked shows prologue when no reminders are set`() = test {
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(false)

        initPrologueBuilderForSiteSettings()

        viewModel.onBlogSettingsItemClicked(siteId)

        assertPrologue()
    }

    @Test
    fun `onSettingsItemClicked shows day selection when reminders are set`() = test {
        whenever(bloggingRemindersStore.hasModifiedBloggingReminders(siteId)).thenReturn(true)

        initEmptyStore()
        initDaySelectionBuilder()

        viewModel.onBlogSettingsItemClicked(siteId)

        assertDaySelection()
    }

    private fun initEmptyStore(): BloggingRemindersUiModel {
        val emptyModel = BloggingRemindersModel(siteId)
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(flowOf(emptyModel))
        return BloggingRemindersUiModel(siteId, hour = hour, minute = minute, isPromptIncluded = false)
    }

    private fun assertPrologue() {
        val state = uiState.last()
        assertPrimaryButton(state.primaryButton!!, R.string.set_your_blogging_reminders_button)
    }

    private fun assertDaySelection(isFirstTime: Boolean = false) {
        val state = uiState.last()
        assertPrimaryButton(
                state.primaryButton!!,
                if (isFirstTime) R.string.blogging_reminders_notify_me else R.string.blogging_reminders_update
        )
    }

    private fun assertEpilogue() {
        val state = uiState.last()
        assertPrimaryButton(state.primaryButton!!, R.string.blogging_reminders_done, isEnabled = true)
    }

    private fun assertPrimaryButton(
        primaryButton: PrimaryButton,
        @StringRes buttonText: Int,
        isEnabled: Boolean = true
    ) {
        assertThat((primaryButton.text as UiStringRes).stringRes).isEqualTo(buttonText)
        assertThat(primaryButton.enabled).isEqualTo(isEnabled)
    }

    private fun clickPrimaryButton() {
        uiState.last().primaryButton!!.onClick.click()
    }

    private fun initDaySelectionBuilder() {
        doAnswer {
            val model = it.getArgument<BloggingRemindersUiModel>(0)
            val onDaySelected: (DayOfWeek) -> Unit = it.getArgument(1)
            listOf(
                    DayButtons(
                            DayOfWeek.values()
                                    .map { day ->
                                        DayItem(
                                                UiStringText(day.name),
                                                model?.enabledDays?.contains(day) == true,
                                                Companion.create { onDaySelected.invoke(day) })
                                    }
                    )
            )
        }.whenever(daySelectionBuilder).buildSelection(any(), any(), any(), any(), any())

        doAnswer {
            val model = it.getArgument<BloggingRemindersUiModel>(0)
            val isFirstTimeFlow = it.getArgument<Boolean>(1)
            val onConfirm: (BloggingRemindersUiModel?) -> Unit = it.getArgument(2)
            val buttonLabel = if (isFirstTimeFlow) {
                R.string.blogging_reminders_notify_me
            } else {
                R.string.blogging_reminders_update
            }
            return@doAnswer PrimaryButton(
                    UiStringRes(buttonLabel),
                    !isFirstTimeFlow || model.enabledDays.isNotEmpty(),
                    ListItemInteraction.create { onConfirm.invoke(model) })
        }.whenever(daySelectionBuilder).buildPrimaryButton(any(), any(), any())
    }

    private fun initPrologueBuilder(): List<BloggingRemindersItem> {
        val uiItems = listOf<BloggingRemindersItem>(Title(UiStringText("Prologue")))
        whenever(prologueBuilder.buildUiItems()).thenReturn(uiItems)
        doAnswer {
            val isFirstTimeFlow = it.getArgument<Boolean>(0)
            val onConfirm: (Boolean) -> Unit = it.getArgument(1)
            PrimaryButton(
                    UiStringRes(R.string.set_your_blogging_reminders_button),
                    true,
                    ListItemInteraction.create { onConfirm.invoke(isFirstTimeFlow) })
        }.whenever(prologueBuilder).buildPrimaryButton(any(), any())
        return uiItems
    }

    private fun initEpilogueBuilder(): List<BloggingRemindersItem> {
        val uiItems = listOf<BloggingRemindersItem>(Title(UiStringText("Epilogue")))
        doAnswer {
            val onConfirm: () -> Unit = it.getArgument(0)
            PrimaryButton(
                    UiStringRes(string.blogging_reminders_done),
                    true,
                    ListItemInteraction.create { onConfirm.invoke() })
        }.whenever(epilogueBuilder).buildPrimaryButton(any())
        return uiItems
    }

    private fun initPrologueBuilderForSiteSettings(): List<BloggingRemindersItem> {
        val uiItems = listOf<BloggingRemindersItem>(Title(UiStringText("Prologue")))
        whenever(prologueBuilder.buildUiItemsForSettings()).thenReturn(uiItems)
        doAnswer {
            val isFirstTimeFlow = it.getArgument<Boolean>(0)
            val onConfirm: (Boolean) -> Unit = it.getArgument(1)
            PrimaryButton(
                    UiStringRes(R.string.set_your_blogging_reminders_button),
                    true,
                    ListItemInteraction.create { onConfirm.invoke(isFirstTimeFlow) })
        }.whenever(prologueBuilder).buildPrimaryButton(any(), any())
        return uiItems
    }
}
