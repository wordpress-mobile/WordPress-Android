package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class StatsColorSelectionViewModelTest : BaseUnitTest() {
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var accountStore: AccountStore
    private lateinit var viewModel: StatsColorSelectionViewModel
    @Before
    fun setUp() {
        viewModel = StatsColorSelectionViewModel(Dispatchers.Unconfined, accountStore, appPrefsWrapper)
    }

    @Test
    fun `updated model on view mode click`() {
        var viewMode: Color? = null
        viewModel.viewMode.observeForever {
            viewMode = it
        }

        viewModel.selectColor(DARK)

        Assertions.assertThat(viewMode).isEqualTo(DARK)

        viewModel.selectColor(LIGHT)

        Assertions.assertThat(viewMode).isEqualTo(LIGHT)
    }

    @Test
    fun `opens dialog when access token present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.dialogOpened.observeForever {
            event = it
        }

        viewModel.openColorDialog()

        assertThat(event).isNotNull
    }

    @Test
    fun `shows notification when access token not present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        var notification: Event<Int>? = null
        viewModel.notification.observeForever {
            notification = it
        }

        viewModel.openColorDialog()

        assertThat(notification).isNotNull
        assertThat(notification?.getContentIfNotHandled()).isEqualTo(R.string.stats_widget_log_in_message)
    }
}
