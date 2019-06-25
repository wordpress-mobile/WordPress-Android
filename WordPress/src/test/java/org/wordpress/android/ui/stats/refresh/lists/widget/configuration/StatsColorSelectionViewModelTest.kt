package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT

class StatsColorSelectionViewModelTest : BaseUnitTest() {
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var viewModel: StatsColorSelectionViewModel
    @Before
    fun setUp() {
        viewModel = StatsColorSelectionViewModel(Dispatchers.Unconfined, appPrefsWrapper)
    }

    @Test
    fun `updated model on view mode click`() {
        var viewMode: Color? = null
        viewModel.viewMode.observeForever {
            viewMode = it
        }

        viewModel.colorClicked(DARK)

        Assertions.assertThat(viewMode).isEqualTo(DARK)

        viewModel.colorClicked(LIGHT)

        Assertions.assertThat(viewMode).isEqualTo(LIGHT)
    }
}
