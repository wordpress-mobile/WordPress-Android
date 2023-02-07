package org.wordpress.android.ui.bloggingreminders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PromptSwitch
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import java.time.DayOfWeek
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.format.TextStyle.SHORT_STANDALONE
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class DaySelectionBuilderTest {
    @Mock
    lateinit var daysProvider: DaysProvider

    @Mock
    lateinit var dayLabelUtils: DayLabelUtils

    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Mock
    lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
    private lateinit var daySelectionBuilder: DaySelectionBuilder
    private var daySelected: DayOfWeek? = null
    private var confirmed = false
    private var promptSwitchToggled = false
    private var bloggingPromptDialogShown = false
    private val hour = 10
    private val minute = 0
    private val onSelectDay: (DayOfWeek) -> Unit = {
        daySelected = it
    }
    private val onSelectTime: () -> Unit = {}
    private val onPromptSwitchToggled: () -> Unit = {
        promptSwitchToggled = true
    }
    private val onConfirm: (BloggingRemindersUiModel?) -> Unit = {
        confirmed = true
    }
    private val onBloggingPromptHelpButtonClicked: () -> Unit = {
        bloggingPromptDialogShown = true
    }

    @Before
    fun setUp() {
        daySelectionBuilder = DaySelectionBuilder(
            daysProvider,
            dayLabelUtils,
            localeManagerWrapper,
            bloggingPromptsFeatureConfig
        )
        whenever(daysProvider.getDaysOfWeekByLocale()).thenReturn(DayOfWeek.values().toList())
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        daySelected = null
        confirmed = false
        promptSwitchToggled = false
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            hour = hour,
            minute = minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Not set")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        assertModel(uiModel, setOf(), dayLabel)
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        assertModel(uiModel, setOf(WEDNESDAY, SUNDAY), dayLabel)
    }

    @Test
    fun `click on a day select day`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            hour = hour,
            minute = minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)).thenReturn(UiStringText("Once a week"))

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        DayOfWeek.values().forEachIndexed { index, day ->
            uiModel.clickOnDayItem(index)
            assertThat(daySelected).isEqualTo(day)
        }
    }

    @Test
    fun `primary button disabled when model is empty and is first time flow`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            hour = hour,
            minute = minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, true, onConfirm)

        assertThat(primaryButton).isEqualTo(
            PrimaryButton(
                UiStringRes(R.string.blogging_reminders_notify_me),
                false,
                Companion.create(bloggingRemindersModel, onConfirm)
            )
        )
    }

    @Test
    fun `primary button enabled when model is not empty and is first time flow`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, true, onConfirm)

        assertThat(primaryButton).isEqualTo(
            PrimaryButton(
                UiStringRes(R.string.blogging_reminders_notify_me),
                true,
                Companion.create(bloggingRemindersModel, onConfirm)
            )
        )
    }

    @Test
    fun `primary button enabled when model is empty and is not first time flow`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            hour = hour,
            minute = minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, false, onConfirm)

        assertThat(primaryButton).isEqualTo(
            PrimaryButton(
                UiStringRes(R.string.blogging_reminders_update),
                true,
                Companion.create(bloggingRemindersModel, onConfirm)
            )
        )
    }

    @Test
    fun `primary button shows a different label when blogging prompt FF is on`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, true, onConfirm)

        assertThat(primaryButton).isEqualTo(
            PrimaryButton(
                UiStringRes(R.string.blogging_prompt_set_reminders),
                true,
                Companion.create(bloggingRemindersModel, onConfirm)
            )
        )
    }

    @Test
    fun `click on primary button confirm selection`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, false, onConfirm)

        primaryButton.onClick.click()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `include prompt switch is visible when days are selected`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val potentialSwitches = uiModel.filterIsInstance<PromptSwitch>()
        assertThat(potentialSwitches.size).isEqualTo(1)
    }

    @Test
    fun `include prompt switch is not visible when days are not selected`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            hour = hour,
            minute = minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Not set")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val potentialSwitches = uiModel.filterIsInstance<PromptSwitch>()
        assertThat(potentialSwitches.size).isEqualTo(0)
    }

    @Test
    fun `single include prompt switch is visible when FF is on`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val potentialSwitches = uiModel.filterIsInstance<PromptSwitch>()
        assertThat(potentialSwitches.size).isEqualTo(1)
    }

    @Test
    fun `include prompt switch is not visible when FF is off`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val potentialSwitches = uiModel.filterIsInstance<PromptSwitch>()
        assertThat(potentialSwitches.isEmpty()).isTrue()
    }

    @Test
    fun `click on a prompt switch toggles the prompt state`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val switch = uiModel.find { it is PromptSwitch }
        (switch as PromptSwitch).onClick.click()

        assertThat(promptSwitchToggled).isTrue()
    }

    @Test
    fun `click on a blogging prompt help button shows blogging prompt dialog`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val bloggingRemindersModel = BloggingRemindersUiModel(
            1,
            setOf(WEDNESDAY, SUNDAY),
            hour,
            minute,
            isPromptIncluded = false,
            isPromptsCardEnabled = false,
        )
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
            .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
            bloggingRemindersModel,
            onSelectDay,
            onSelectTime,
            onPromptSwitchToggled,
            onBloggingPromptHelpButtonClicked
        )

        val switch = uiModel.find { it is PromptSwitch }
        (switch as PromptSwitch).onHelpClick.click()

        assertThat(bloggingPromptDialogShown).isTrue()
    }

    private fun assertModel(
        uiModel: List<BloggingRemindersItem>,
        selectedDays: Set<DayOfWeek>,
        dayLabel: UiString
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(R.drawable.img_illustration_calendar))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(R.string.blogging_reminders_select_days)))
        assertThat(uiModel[2]).isEqualTo(
            MediumEmphasisText(UiStringRes(R.string.blogging_reminders_select_days_message))
        )
        assertThat(uiModel[3]).isEqualTo(
            DayButtons(
                DayOfWeek.values().map {
                    DayItem(
                        UiStringText(it.getDisplayName(SHORT_STANDALONE, Locale.US)),
                        selectedDays.contains(it),
                        ListItemInteraction.create(it, onSelectDay)
                    )
                }
            )
        )

        assertThat(uiModel[4]).isEqualTo(MediumEmphasisText(EmphasizedText(dayLabel), selectedDays.isEmpty()))
    }

    private fun List<BloggingRemindersItem>.clickOnDayItem(position: Int) {
        (this.find { it is DayButtons } as DayButtons).dayItems[position].onClick.click()
    }
}
