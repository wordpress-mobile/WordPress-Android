package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.format.TextStyle.SHORT_STANDALONE
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class DaySelectionBuilderTest {
    @Mock lateinit var daysProvider: DaysProvider
    @Mock lateinit var dayLabelUtils: DayLabelUtils
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var daySelectionBuilder: DaySelectionBuilder
    private var daySelected: DayOfWeek? = null
    private var confirmed = false
    private val hour = 10
    private val minute = 0
    private val onSelectDay: (DayOfWeek) -> Unit = {
        daySelected = it
    }
    private val onSelectTime: () -> Unit = {}
    private val onConfirm: (BloggingRemindersUiModel?) -> Unit = {
        confirmed = true
    }

    @Before
    fun setUp() {
        daySelectionBuilder = DaySelectionBuilder(daysProvider, dayLabelUtils, localeManagerWrapper)
        whenever(daysProvider.getDaysOfWeekByLocale()).thenReturn(DayOfWeek.values().toList())
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        daySelected = null
        confirmed = false
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(1, hour = hour, minute = minute)
        val dayLabel = UiStringText("Not set")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
                .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay, onSelectTime)

        assertModel(uiModel, setOf(), dayLabel)
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(1, setOf(WEDNESDAY, SUNDAY), hour, minute)
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
                .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
                bloggingRemindersModel,
                onSelectDay,
                onSelectTime
        )

        assertModel(uiModel, setOf(WEDNESDAY, SUNDAY), dayLabel)
    }

    @Test
    fun `click on a day select day`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(1, hour = hour, minute = minute)
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)).thenReturn(UiStringText("Once a week"))

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay, onSelectTime)

        DayOfWeek.values().forEachIndexed { index, day ->
            uiModel.clickOnDayItem(index)
            assertThat(daySelected).isEqualTo(day)
        }
    }

    @Test
    fun `primary button disabled when model is empty and is first time flow`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(1, hour = hour, minute = minute)

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
        val bloggingRemindersModel = BloggingRemindersUiModel(1, setOf(WEDNESDAY, SUNDAY), hour, minute)

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
        val bloggingRemindersModel = BloggingRemindersUiModel(1, hour = hour, minute = minute)

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
    fun `click on primary button confirm selection`() {
        val bloggingRemindersModel = BloggingRemindersUiModel(1, setOf(WEDNESDAY, SUNDAY), hour, minute)

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, false, onConfirm)

        primaryButton.onClick.click()

        assertThat(confirmed).isTrue()
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
