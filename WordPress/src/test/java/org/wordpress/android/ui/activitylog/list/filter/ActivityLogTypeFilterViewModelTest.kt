package org.wordpress.android.ui.activitylog.list.filter

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.test
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.ActivityType
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.DummyActivityTypesProvider.DummyActivityType
import org.wordpress.android.ui.activitylog.list.filter.DummyActivityTypesProvider.DummyAvailableActivityTypesResponse
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel

@InternalCoroutinesApi
class ActivityLogTypeFilterViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ActivityLogTypeFilterViewModel
    @Mock private lateinit var dummyActivityTypesProvider: DummyActivityTypesProvider
    @Mock private lateinit var parentViewModel: ActivityLogViewModel

    @Before
    fun setUp() {
        viewModel = ActivityLogTypeFilterViewModel(dummyActivityTypesProvider, TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `fullscreen loading shown, when screen initialized`() = test {
        val uiStates = init().uiStates

        startVM()

        assertThat(uiStates[0]).isInstanceOf(UiState.FullscreenLoading::class.java)
    }

    @Test
    fun `available activity types fetched, when screen initialized, when content shown`() = test {
        init()

        startVM()

        verify(dummyActivityTypesProvider).fetchAvailableActivityTypes(anyOrNull())
    }

    @Test
    fun `section header gets added as first item in the list, when content shown`() = test {
        init()

        startVM()

        assertThat((viewModel.uiState.value as Content).items[0])
                .isInstanceOf(ListItemUiState.SectionHeader::class.java)
    }

    @Test
    fun `content shown, when fetch available activity types completes successfully`() = test {
        init(successResponse = true)

        startVM()

        assertThat(viewModel.uiState.value).isInstanceOf(Content::class.java)
    }

    @Test
    fun `fullscreen error shown, when fetch available activity types completes with error`() = test {
        init(successResponse = false)

        startVM()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `available activity types fetched, when error retry action invoked`() = test {
        init(successResponse = false)
        startVM()

        (viewModel.uiState.value as UiState.Error).retryAction.action.invoke()

        verify(dummyActivityTypesProvider, times(2)).fetchAvailableActivityTypes(anyOrNull())
    }

    @Test
    fun `content shown, when retry succeeds`() = test {
        init(successResponse = false)
        startVM()
        init(successResponse = true)

        (viewModel.uiState.value as UiState.Error).retryAction.action.invoke()

        assertThat(viewModel.uiState.value).isInstanceOf(Content::class.java)
    }

    @Test
    fun `content contains all fetched activity types, when fetch activity types completes`() = test {
        val activityTypeCount = 17 // random number
        init(activityTypeCount = activityTypeCount)

        startVM()

        assertThat((viewModel.uiState.value as Content).items.size).isEqualTo(1 + activityTypeCount)
    }

    @Test
    fun `item is checked, when the user clicks on it`() = test {
        val uiStates = init().uiStates
        startVM()

        ((uiStates.last() as Content).items[1] as ActivityType).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1] as ActivityType).checked).isTrue()
    }

    @Test
    fun `item is unchecked, when the user clicks on it twice`() = test {
        val uiStates = init().uiStates
        startVM()

        ((uiStates.last() as Content).items[1] as ActivityType).onClick.invoke()
        ((uiStates.last() as Content).items[1] as ActivityType).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1] as ActivityType).checked).isFalse()
    }

    @Test
    fun `dialog dismissed, when the user clicks on apply action`() = test {
        val observers = init()
        startVM()

        (observers.uiStates.last() as Content).primaryAction.action.invoke()

        assertThat(observers.dismissDialogEvents).isNotEmpty
    }

    @Test
    fun `selected items propagated to activity log, when the user clicks on apply action`() = test {
        val observers = init()
        startVM()
        // select an item
        val activityType = ((observers.uiStates.last() as Content).items[1] as ActivityType)
        activityType.onClick.invoke()

        (observers.uiStates.last() as Content).primaryAction.action.invoke()

        verify(parentViewModel).onActivityTypesSelected(listOf(activityType.id))
    }

    @Test
    fun `items unchecked, when the user clicks on clear action`() = test {
        val uiStates = init().uiStates
        startVM()
        // select an item
        val activityType = ((uiStates.last() as Content).items[1] as ActivityType)
        activityType.onClick.invoke()

        (uiStates.last() as Content).secondaryAction.action.invoke()

        assertThat(
                (uiStates.last() as Content).items.filterIsInstance(ActivityType::class.java)
                        .filter { it.checked }
        ).isEmpty()
    }

    @Test
    fun `items are checked, when the user opens the screen with active activity type filter`() = test {
        val uiStates = init().uiStates
        val initialSelection = listOf(1, 4)

        startVM(initialSelection = initialSelection)

        assertThat((uiStates.last() as Content).items.filterIsInstance(ActivityType::class.java)
                        .filter { it.checked }.map { it.id }
        ).containsExactlyElementsOf(initialSelection)
    }

    private suspend fun init(successResponse: Boolean = true, activityTypeCount: Int = 5): Observers {
        val uiStates = mutableListOf<UiState>()
        val dismissDialogEvents = mutableListOf<Unit>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.dismissDialog.observeForever {
            dismissDialogEvents.add(it.peekContent())
        }

        whenever(dummyActivityTypesProvider.fetchAvailableActivityTypes(anyOrNull()))
                .thenReturn(
                        if (successResponse) {
                            DummyAvailableActivityTypesResponse(false, generateActivityTypes(activityTypeCount))
                        } else {
                            DummyAvailableActivityTypesResponse(true, listOf())
                        }
                )
        return Observers(uiStates, dismissDialogEvents)
    }

    private fun startVM(initialSelection: List<Int> = listOf()) {
        viewModel.start(RemoteId(0L), parentViewModel, initialSelection)
    }

    private fun generateActivityTypes(count: Int): List<DummyActivityType> {
        return (1..count).asSequence().map { DummyActivityType(it, it.toString()) }.toList()
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val dismissDialogEvents: List<Unit>
    )
}
