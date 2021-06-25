package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.FRIDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SATURDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.THURSDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.TUESDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.ListFormatterUtils
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class EpilogueBuilderTest {
    @Mock lateinit var dayLabelUtils: DayLabelUtils
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var listFormatterUtils: ListFormatterUtils
    private lateinit var epilogueBuilder: EpilogueBuilder
    private var done = false

    private val onDone: () -> Unit = {
        done = true
    }

    @Before
    fun setUp() {
        epilogueBuilder = EpilogueBuilder(dayLabelUtils, localeManagerWrapper, listFormatterUtils)
        done = false
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `builds UI model with no selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf())
        val uiModel = epilogueBuilder.buildUiItems(bloggingRemindersModel)

        assertModelWithNoSelection(uiModel)
    }

    @Test
    fun `builds UI model with selected days`() {
        val bloggingRemindersModel = BloggingRemindersModel(1, setOf(WEDNESDAY, SUNDAY))
        val dayLabel = UiStringText("twice")
        whenever(dayLabelUtils.buildLowercaseNTimesLabel(bloggingRemindersModel))
                .thenReturn(dayLabel)
        val selectedDays = "Wednesday, Sunday"
        whenever(listFormatterUtils.formatList(listOf("Wednesday", "Sunday"))).thenReturn(selectedDays)

        val uiModel = epilogueBuilder.buildUiItems(bloggingRemindersModel)

        assertModelWithSelection(uiModel, dayLabel, selectedDays)
    }

    @Test
    fun `builds UI model with all days selected`() {
        val bloggingRemindersModel = BloggingRemindersModel(
                1,
                setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
        )
        val uiModel = epilogueBuilder.buildUiItems(bloggingRemindersModel)

        assertModelWithAllDaysSelection(uiModel)
    }

    @Test
    fun `builds primary button`() {
        val primaryButton = epilogueBuilder.buildPrimaryButton(onDone)

        assertThat(primaryButton).isEqualTo(
                PrimaryButton(
                        UiStringRes(string.blogging_reminders_done),
                        true,
                        ListItemInteraction.create(onDone)
                )
        )
    }

    @Test
    fun `click on primary button dismisses bottomsheet`() {
        val primaryButton = epilogueBuilder.buildPrimaryButton(onDone)

        primaryButton.onClick.click()

        assertThat(done).isTrue
    }

    private fun assertModelWithNoSelection(
        uiModel: List<BloggingRemindersItem>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(drawable.img_illustration_bell_yellow_96dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(string.blogging_reminders_epilogue_not_set_title)))
        assertThat(uiModel[2])
                .isEqualTo(HighEmphasisText(UiStringRes(string.blogging_reminders_epilogue_body_no_reminders)))
    }

    private fun assertModelWithSelection(
        uiModel: List<BloggingRemindersItem>,
        numberOfTimes: UiString,
        selectedDays: String
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(drawable.img_illustration_bell_yellow_96dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(string.blogging_reminders_epilogue_title)))
        assertThat(uiModel[2])
                .isEqualTo(
                        HighEmphasisText(
                                UiStringResWithParams(
                                        string.blogging_reminders_epilogue_body_days,
                                        listOf(numberOfTimes, UiStringText(selectedDays))
                                )
                        )
                )
    }

    private fun assertModelWithAllDaysSelection(
        uiModel: List<BloggingRemindersItem>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(drawable.img_illustration_bell_yellow_96dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(string.blogging_reminders_epilogue_title)))
        assertThat(uiModel[2]).isEqualTo(
                HighEmphasisText(
                        UiStringRes(string.blogging_reminders_epilogue_body_everyday)
                )
        )
    }
}
