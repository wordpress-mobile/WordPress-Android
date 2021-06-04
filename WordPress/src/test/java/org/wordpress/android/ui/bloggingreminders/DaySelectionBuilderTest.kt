package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
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
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class DaySelectionBuilderTest {
    @Mock lateinit var daysProvider: DaysProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var daySelectionBuilder: DaySelectionBuilder
    private var daySelected: Day? = null
    private var confirmed = false
    private val once = "once"
    private val twice = "twice"

    private val onSelectDay: (Day) -> Unit = {
        daySelected = it
    }
    private val onConfirm: (BloggingRemindersModel?) -> Unit = {
        confirmed = true
    }

    @Before
    fun setUp() {
        daySelectionBuilder = DaySelectionBuilder(daysProvider, resourceProvider)
        whenever(daysProvider.getDays()).thenReturn(Day.values().map {
            it.name to it
        })
        daySelected = null
        confirmed = false
        whenever(resourceProvider.getStringArray(R.array.blogging_goals_count)).thenReturn(
                arrayOf(once, twice)
        )
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay)

        assertModel(uiModel, setOf())
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

        val uiModel = daySelectionBuilder.buildSelection(
                bloggingRemindersModel,
                onSelectDay
        )

        assertModel(uiModel, setOf(WEDNESDAY, SUNDAY))
    }

    @Test
    fun `click on a day select day`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay)

        Day.values().forEachIndexed { index, day ->
            uiModel.clickOnDayItem(index)
            assertThat(daySelected).isEqualTo(day)
        }
    }

    @Test
    fun `primary button disabled when model is empty`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, onConfirm)

        assertThat(primaryButton).isEqualTo(
                PrimaryButton(
                        UiStringRes(string.blogging_reminders_notify_me),
                        false,
                        Companion.create(bloggingRemindersModel, onConfirm)
                )
        )
    }

    @Test
    fun `primary button enabled when model is not empty`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, onConfirm)

        assertThat(primaryButton).isEqualTo(
                PrimaryButton(
                        UiStringRes(string.blogging_reminders_notify_me),
                        true,
                        Companion.create(bloggingRemindersModel, onConfirm)
                )
        )
    }

    @Test
    fun `click on primary button confirm selection`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

        val primaryButton = daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, onConfirm)

        primaryButton.onClick.click()

        assertThat(confirmed).isTrue()
    }

    private fun assertModel(
        uiModel: List<BloggingRemindersItem>,
        selectedDays: Set<Day>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(drawable.img_illustration_calendar))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(string.blogging_reminders_select_days)))
        assertThat(uiModel[2]).isEqualTo(MediumEmphasisText(UiStringRes(string.blogging_reminders_select_days_message)))
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
        val param = when (selectedDays.size) {
            0 -> null
            1 -> once
            else -> twice
        }
        val text = param?.let {
            UiStringResWithParams(
                    string.blogging_goals_n_a_week,
                    listOf(UiStringText(param))
            )
        }

        assertThat(uiModel[4]).isEqualTo(MediumEmphasisText(text?.let { EmphasizedText(text) }, text == null))
    }

    private fun List<BloggingRemindersItem>.clickOnDayItem(position: Int) {
        (this.find { it is DayButtons } as DayButtons).dayItems[position].onClick.click()
    }
}
