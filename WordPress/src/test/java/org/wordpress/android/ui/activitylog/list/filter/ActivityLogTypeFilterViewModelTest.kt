package org.wordpress.android.ui.activitylog.list.filter

import androidx.core.util.Pair
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITY_TYPES
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityTypesError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityTypesErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityTypesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityTypesFetched
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.ActivityType
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import org.wordpress.android.viewmodel.activitylog.DateRange

private const val REMOTE_SITE_ID = 0L

@ExperimentalCoroutinesApi
class ActivityLogTypeFilterViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ActivityLogTypeFilterViewModel
    @Mock private lateinit var parentViewModel: ActivityLogViewModel
    @Mock private lateinit var activityLogStore: ActivityLogStore

    @Before
    fun setUp() {
        viewModel = ActivityLogTypeFilterViewModel(
                activityLogStore,
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher
        )
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

        verify(activityLogStore).fetchActivityTypes(anyOrNull())
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
    fun `connection error shown, when fetch available activity types completes with error`() = test {
        init(successResponse = false)

        startVM()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error.ConnectionError::class.java)
    }

    @Test
    fun `no activities error shown, when fetch available activity types returns empty list`() = test {
        init(successResponse = true, activityTypeCount = 0)

        startVM()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error.NoActivitiesError::class.java)
    }

    @Test
    fun `available activity types fetched, when error retry action invoked`() = test {
        init(successResponse = false)
        startVM()

        (viewModel.uiState.value as UiState.Error).retryAction!!.action.invoke()

        verify(activityLogStore, times(2)).fetchActivityTypes(anyOrNull())
    }

    @Test
    fun `content shown, when retry succeeds`() = test {
        init(successResponse = false)
        startVM()
        init(successResponse = true)

        (viewModel.uiState.value as UiState.Error).retryAction!!.action.invoke()

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

        assertThat(((uiStates.last() as Content).items[1] as ActivityType).checked).isTrue
    }

    @Test
    fun `item is unchecked, when the user clicks on it twice`() = test {
        val uiStates = init().uiStates
        startVM()

        ((uiStates.last() as Content).items[1] as ActivityType).onClick.invoke()
        ((uiStates.last() as Content).items[1] as ActivityType).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1] as ActivityType).checked).isFalse
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
        val observers = init(activityTypeCount = 1)
        startVM()
        // select an item
        val activityType = ((observers.uiStates.last() as Content).items[1] as ActivityType)
        activityType.onClick.invoke()

        (observers.uiStates.last() as Content).primaryAction.action.invoke()

        verify(parentViewModel).onActivityTypesSelected(listOf(ActivityTypeModel("1", "1", 1)))
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
        val initialSelection = listOf("1", "4")

        startVM(initialSelection = initialSelection)

        assertThat((uiStates.last() as Content).items.filterIsInstance(ActivityType::class.java)
                .filter { it.checked }.map { it.id }
        ).containsExactlyElementsOf(initialSelection)
    }

    @Test
    fun `date range passed to the api, when provided on view model start`() = test {
        val after = 1L
        val before = 2L
        init().uiStates
        val captor: KArgumentCaptor<FetchActivityTypesPayload> = argumentCaptor()

        startVM(dateRange = Pair(after, before))

        verify(activityLogStore).fetchActivityTypes(captor.capture())
        assertThat(captor.firstValue.after!!.time).isEqualTo(after)
        assertThat(captor.firstValue.before!!.time).isEqualTo(before)
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

        whenever(activityLogStore.fetchActivityTypes(anyOrNull()))
                .thenReturn(
                        if (successResponse) {
                            OnActivityTypesFetched(
                                    FETCH_ACTIVITY_TYPES,
                                    REMOTE_SITE_ID,
                                    generateActivityTypes(activityTypeCount),
                                    activityTypeCount
                            )
                        } else {
                            OnActivityTypesFetched(
                                    REMOTE_SITE_ID,
                                    ActivityTypesError(GENERIC_ERROR),
                                    FETCH_ACTIVITY_TYPES
                            )
                        }
                )
        return Observers(uiStates, dismissDialogEvents)
    }

    private fun startVM(initialSelection: List<String> = listOf(), dateRange: DateRange? = null) {
        viewModel.start(RemoteId(REMOTE_SITE_ID), parentViewModel, dateRange, initialSelection)
    }

    private fun generateActivityTypes(count: Int): List<ActivityTypeModel> {
        return (1..count).asSequence().map { ActivityTypeModel("$it", it.toString(), count) }.toList()
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val dismissDialogEvents: List<Unit>
    )
}
