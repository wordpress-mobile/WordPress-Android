package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

@RunWith(MockitoJUnitRunner::class)
class DaySelectionBuilderTest {
    @Mock lateinit var daysProvider: DaysProvider
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
        daySelectionBuilder = DaySelectionBuilder(daysProvider)
        whenever(daysProvider.getDays()).thenReturn(Day.values().map {
            it.name to it
        })
        daySelected = null
        confirmed = false
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay, onConfirm)

        assertModel(bloggingRemindersModel, uiModel, setOf())
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))

        val uiModel = daySelectionBuilder.buildSelection(
                bloggingRemindersModel,
                onSelectDay,
                onConfirm
        )

        assertModel(bloggingRemindersModel, uiModel, setOf(WEDNESDAY, SUNDAY))
    }

    @Test
    fun `click on a day select day`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay, onConfirm)

        Day.values().forEachIndexed { index, day ->
            uiModel.clickOnDayItem(index)
            assertThat(daySelected).isEqualTo(day)
        }
    }

    @Test
    fun `click on primary button confirm selection`() {
        val bloggingRemindersModel = BloggingRemindersModel(1)

        val uiModel = daySelectionBuilder.buildSelection(bloggingRemindersModel, onSelectDay, onConfirm)

        uiModel.clickOnPrimaryButton()

        assertThat(confirmed).isTrue()
    }

    private fun assertModel(
        domainModel: BloggingRemindersModel,
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
        assertThat(uiModel[4]).isEqualTo(
                PrimaryButton(
                        UiStringRes(string.blogging_reminders_notify_me),
                        selectedDays.isNotEmpty(),
                        Companion.create(domainModel, onConfirm)
                )
        )
    }

    private fun List<BloggingRemindersItem>.clickOnDayItem(position: Int) {
        (this.find { it is DayButtons } as DayButtons).dayItems[position].onClick.click()
    }

    private fun List<BloggingRemindersItem>.clickOnPrimaryButton() {
        (this.find { it is PrimaryButton } as PrimaryButton).onClick.click()
    }
}
