package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
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

@RunWith(MockitoJUnitRunner::class)
class DaySelectionBuilderTest {
    @Mock lateinit var daysProvider: DaysProvider
    @Mock lateinit var dayLabelUtils: DayLabelUtils
    private lateinit var daySelectionBuilder: DaySelectionBuilder
    private var daySelected: Day? = null
    private var confirmed = false

    private val onSelectDay: (Day) -> Unit = {
        daySelected = it
    }
    private val onConfirm: (BloggingRemindersModel?) -> Unit = {
        confirmed = true
    }

    @Before
    fun setUp() {
        daySelectionBuilder = DaySelectionBuilder(daysProvider, dayLabelUtils)
        whenever(daysProvider.getDays()).thenReturn(Day.values().map {
            it.name to it
        })
        daySelected = null
        confirmed = false
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)
        val dayLabel = UiStringText("Not set")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
                .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay)

        assertModel(uiModel, setOf(), dayLabel)
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))
        val dayLabel = UiStringText("Twice a week")
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel))
                .thenReturn(dayLabel)

        val uiModel = daySelectionBuilder.buildSelection(
                bloggingRemindersModel,
                onSelectDay
        )

        assertModel(uiModel, setOf(WEDNESDAY, SUNDAY), dayLabel)
    }

    @Test
    fun `click on a day select day`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)
        whenever(dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)).thenReturn(UiStringText("Once a week"))

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay)

        Day.values().forEachIndexed { index, day ->
            uiModel.clickOnDayItem(index)
            assertThat(daySelected).isEqualTo(day)
        }
    }

    @Test
    fun `primary button disabled when model is empty and is first time flow`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

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
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

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
        val bloggingRemindersModel = BloggingRemindersModel(1)

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
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, false, onConfirm)

        primaryButton.onClick.click()

        assertThat(confirmed).isTrue()
    }

    private fun assertModel(
        uiModel: List<BloggingRemindersItem>,
        selectedDays: Set<Day>,
        dayLabel: UiString
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(R.drawable.img_illustration_calendar))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(R.string.blogging_reminders_select_days)))
        assertThat(uiModel[2]).isEqualTo(MediumEmphasisText(UiStringRes(R.string.blogging_reminders_select_days_message)))
        assertThat(uiModel[3]).isEqualTo(
                DayButtons(
                        Day.values().map {
                            DayItem(
                                    UiStringText(it.name),
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
