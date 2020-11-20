package org.wordpress.android.ui.activitylog.list.filter

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content

@InternalCoroutinesApi
class ActivityLogTypeFilterViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ActivityLogTypeFilterViewModel

    @Before
    fun setUp() {
        viewModel = ActivityLogTypeFilterViewModel(TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `fullscreen loading shown, when screen initialized`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start()

        assertThat(uiStates[0]).isInstanceOf(UiState.FullscreenLoading::class.java)
    }

    @Test
    fun `section header gets added as first item in the list`() {
        val uiStates = initObservers().uiStates

        viewModel.start()

        assertThat((viewModel.uiState.value as Content).items[0])
                .isInstanceOf(ListItemUiState.SectionHeader::class.java)
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        return Observers((uiStates))
    }

    private data class Observers(val uiStates: List<UiState>)
}
