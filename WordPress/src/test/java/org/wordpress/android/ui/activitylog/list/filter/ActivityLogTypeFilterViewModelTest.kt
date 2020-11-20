package org.wordpress.android.ui.activitylog.list.filter

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
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

        viewModel.start(0L)

        assertThat(uiStates[0]).isInstanceOf(UiState.FullscreenLoading::class.java)
    }

    @Test
    fun `section header gets added as first item in the list`() = test {
        val uiStates = init().uiStates

        viewModel.start(0L)

        assertThat((viewModel.uiState.value as Content).items[0])
                .isInstanceOf(ListItemUiState.SectionHeader::class.java)
    }

    private suspend fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        whenever(dummyActivityTypesProvider.fetchAvailableActivityTypes(anyOrNull()))
                .thenReturn(DummyAvailableActivityTypesResponse(false, listOf(DummyActivityType("Test 1"))))
        return Observers((uiStates))
    }

    private data class Observers(val uiStates: List<UiState>)
}
