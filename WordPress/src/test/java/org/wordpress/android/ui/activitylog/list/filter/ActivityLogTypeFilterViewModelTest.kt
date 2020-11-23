package org.wordpress.android.ui.activitylog.list.filter

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
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
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState
import org.wordpress.android.ui.activitylog.list.filter.DummyActivityTypesProvider.DummyActivityType
import org.wordpress.android.ui.activitylog.list.filter.DummyActivityTypesProvider.DummyAvailableActivityTypesResponse

@InternalCoroutinesApi
class ActivityLogTypeFilterViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ActivityLogTypeFilterViewModel
    @Mock private lateinit var dummyActivityTypesProvider: DummyActivityTypesProvider

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
    fun `available activity types fetched, when screen initialized`() = test {
        init()

        startVM()

        verify(dummyActivityTypesProvider).fetchAvailableActivityTypes(anyOrNull())
        verifyNoMoreInteractions(dummyActivityTypesProvider)
    }

    @Test
    fun `section header gets added as first item in the list`() = test {
        init()

        startVM()

        assertThat((viewModel.uiState.value as UiState.Content).items[0])
                .isInstanceOf(ListItemUiState.SectionHeader::class.java)
    }

    @Test
    fun `content shown, when fetch available activity types completes successfully`() = test {
        val uiStates = init(successResponse = true).uiStates

        startVM()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Content::class.java)
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

        (viewModel.uiState.value as UiState.Error).retryAction.action!!.invoke()

        verify(dummyActivityTypesProvider, times(2)).fetchAvailableActivityTypes(anyOrNull())
    }

    @Test
    fun `content shown, when retry succeeds`() = test {
        init(successResponse = false)
        startVM()
        init(successResponse = true)

        (viewModel.uiState.value as UiState.Error).retryAction.action!!.invoke()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Content::class.java)
    }

    @Test
    fun `content contains all fetched activity types`() = test {
        val activityTypeCount = 17 // random number
        init(activityTypeCount = activityTypeCount)

        startVM()

        assertThat((viewModel.uiState.value as UiState.Content).items.size).isEqualTo(1 + activityTypeCount)
    }

    private suspend fun init(successResponse: Boolean = true, activityTypeCount: Int = 5): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }

        whenever(dummyActivityTypesProvider.fetchAvailableActivityTypes(anyOrNull()))
                .thenReturn(
                        if (successResponse) {
                            DummyAvailableActivityTypesResponse(false, generateActivityTypes(activityTypeCount))
                        } else {
                            DummyAvailableActivityTypesResponse(true, listOf())
                        }
                )
        return Observers((uiStates))
    }

    private fun startVM() {
        viewModel.start(RemoteId(0L))
    }

    private fun generateActivityTypes(count: Int): List<DummyActivityType> {
        return (1..count).asSequence().map { DummyActivityType(it.toString()) }.toList()
    }

    private data class Observers(val uiStates: List<UiState>)
}
