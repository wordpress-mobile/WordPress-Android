package org.wordpress.android.ui.bloggingreminders

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
import org.wordpress.android.viewmodel.ResourceProvider

class BloggingRemindersViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingRemindersManager: BloggingRemindersManager
    @Mock lateinit var bloggingRemindersStore: BloggingRemindersStore
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var prologueBuilder: PrologueBuilder
    @Mock lateinit var daySelectionBuilder: DaySelectionBuilder
    @Mock lateinit var dayLabelUtils: DayLabelUtils
    @Mock lateinit var analyticsTracker: BloggingRemindersAnalyticsTracker
    private lateinit var viewModel: BloggingRemindersViewModel
    private val siteId = 123
    private lateinit var events: MutableList<Boolean>
    private lateinit var uiState: MutableList<UiState>

    @ExperimentalStdlibApi
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = BloggingRemindersViewModel(
                TEST_DISPATCHER,
                bloggingRemindersManager,
                bloggingRemindersStore,
                resourceProvider,
                prologueBuilder,
                daySelectionBuilder,
                dayLabelUtils,
                analyticsTracker
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
        val uiItems = initPrologueBuilder()

        viewModel.showBottomSheet(siteId, PROLOGUE)

        assertThat(uiState.last().uiItems).isEqualTo(uiItems)
    }

    @Test
    fun `date selection selected`() {
        val model = initEmptyStore()
        val daySelectionScreen = listOf<BloggingRemindersItem>()
        whenever(daySelectionBuilder.buildSelection(eq(model), any())).thenReturn(daySelectionScreen)

        viewModel.showBottomSheet(siteId, SELECTION)

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
        val dayLabel = UiStringText("Blogging reminders 2 times a week")
        whenever(
                dayLabelUtils.buildNTimesLabel(model)
        ).thenReturn(dayLabel)
        var uiState: UiString? = null

        viewModel.getSettingsState(siteId).observeForever { uiState = it }

        assertThat(uiState).isEqualTo(dayLabel)
    }

    @Test
    fun `switches from prologue do day selection on primary button click`() {
        initPrologueBuilder()

        viewModel.showBottomSheet(siteId, PROLOGUE)

        clickPrimaryButton()

        assertThat(uiState.last().uiItems).isEqualTo(listOf<BloggingRemindersItem>())
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
            val model = it.getArgument<BloggingRemindersModel>(0)
            val onDaySelected: (Day) -> Unit = it.getArgument(1)
            listOf(
                    DayButtons(
                            Day.values()
                                    .map { day ->
                                        DayItem(
                                                UiStringText(day.name),
                                                model?.enabledDays?.contains(day) == true,
                                                Companion.create { onDaySelected.invoke(day) })
                                    }
                    )
            )
        }.whenever(daySelectionBuilder).buildSelection(any(), any())

        doAnswer {
            val model = it.getArgument<BloggingRemindersModel>(0)
            val isFirstTimeFlow = it.getArgument<Boolean>(1)
            val onConfirm: (BloggingRemindersModel?) -> Unit = it.getArgument(2)
            PrimaryButton(
                    UiStringText("Confirm"),
                    !isFirstTimeFlow || model.enabledDays.isNotEmpty(),
                    ListItemInteraction.create { onConfirm.invoke(model) })
        }.whenever(daySelectionBuilder).buildPrimaryButton(any(), any(), any())
    }

    private fun initPrologueBuilder(): List<BloggingRemindersItem> {
        val uiItems = listOf<BloggingRemindersItem>(Title(UiStringText("Prologue")))
        whenever(prologueBuilder.buildUiItems()).thenReturn(uiItems)
        doAnswer {
            val onConfirm: () -> Unit = it.getArgument(0)
            PrimaryButton(
                    UiStringText("Confirm"),
                    true,
                    ListItemInteraction.create { onConfirm.invoke() })
        }.whenever(prologueBuilder).buildPrimaryButton(any())
        return uiItems
    }
}
