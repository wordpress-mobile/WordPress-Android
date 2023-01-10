package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class StatsDataTypeSelectionViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var accountStore: AccountStore
    private lateinit var viewModel: StatsDataTypeSelectionViewModel

    @Before
    fun setUp() {
        viewModel = StatsDataTypeSelectionViewModel(
            testDispatcher(),
            accountStore,
            appPrefsWrapper
        )
    }

    @Test
    fun `opens dialog when access token present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.dialogOpened.observeForever {
            event = it
        }

        viewModel.openDataTypeDialog()

        Assertions.assertThat(event).isNotNull
    }

    @Test
    fun `shows notification when access token not present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        var notification: Event<Int>? = null
        viewModel.notification.observeForever {
            notification = it
        }

        viewModel.openDataTypeDialog()

        Assertions.assertThat(notification).isNotNull
        val message = if (BuildConfig.IS_JETPACK_APP) {
            R.string.stats_widget_log_in_to_add_message
        } else {
            R.string.stats_widget_log_in_message
        }
        Assertions.assertThat(notification?.getContentIfNotHandled()).isEqualTo(message)
    }
}
