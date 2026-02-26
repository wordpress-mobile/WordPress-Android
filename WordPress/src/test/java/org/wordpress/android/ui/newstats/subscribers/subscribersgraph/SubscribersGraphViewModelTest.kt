package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class SubscribersGraphViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: SubscribersGraphViewModel

    private fun initViewModel() {
        viewModel = SubscribersGraphViewModel()
    }

    @Test
    fun `when initialized, then ui state is Placeholder`() = test {
        initViewModel()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(SubscribersGraphUiState.Placeholder::class.java)
    }

    @Test
    fun `when initialized, then isRefreshing is false`() = test {
        initViewModel()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when loadDataIfNeeded is called, then state remains Placeholder`() = test {
        initViewModel()

        viewModel.loadDataIfNeeded()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(SubscribersGraphUiState.Placeholder::class.java)
    }

    @Test
    fun `when refresh is called, then state remains Placeholder`() = test {
        initViewModel()

        viewModel.refresh()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(SubscribersGraphUiState.Placeholder::class.java)
    }

    @Test
    fun `when loadData is called, then state remains Placeholder`() = test {
        initViewModel()

        viewModel.loadData()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(SubscribersGraphUiState.Placeholder::class.java)
    }

    @Test
    fun `when refresh is called, then isRefreshing remains false`() = test {
        initViewModel()

        viewModel.refresh()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }
}
