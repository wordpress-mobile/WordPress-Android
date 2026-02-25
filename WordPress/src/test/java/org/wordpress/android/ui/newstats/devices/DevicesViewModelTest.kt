package org.wordpress.android.ui.newstats.devices

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.DeviceItemData
import org.wordpress.android.ui.newstats.repository.DevicesResult
import org.wordpress.android.ui.newstats.repository.StatsRepository

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class DevicesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    private lateinit var viewModel: DevicesViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
    }

    private fun initViewModel() {
        viewModel = DevicesViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() =
        test {
            whenever(selectedSiteRepository.getSelectedSite())
                .thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Error::class.java
            )
            assertThat(
                (state as DevicesCardUiState.Error).messageResId
            ).isEqualTo(R.string.stats_error_no_site)
        }

    @Test
    fun `when no access token, then error state is emitted`() =
        test {
            whenever(accountStore.accessToken).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Error::class.java
            )
            assertThat(
                (state as DevicesCardUiState.Error).messageResId
            ).isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(
            DevicesResult.Error(R.string.stats_error_api)
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(
            DevicesCardUiState.Error::class.java
        )
        assertThat(
            (state as DevicesCardUiState.Error).messageResId
        ).isEqualTo(R.string.stats_error_api)
    }

    @Test
    fun `when auth error, then isAuthError is true`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(
            DevicesResult.Error(
                R.string.stats_error_auth,
                isAuthError = true
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(
            DevicesCardUiState.Error::class.java
        )
        assertThat(
            (state as DevicesCardUiState.Error).isAuthError
        ).isTrue()
    }

    @Test
    fun `when browser fetch fails, then error state is emitted`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())
            whenever(
                statsRepository.fetchDevicesBrowser(any(), any())
            ).thenReturn(
                DevicesResult.Error(R.string.stats_error_api)
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.onDeviceTypeChanged(DeviceType.BROWSER)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Error::class.java
            )
        }

    @Test
    fun `when platform fetch fails, then error state is emitted`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())
            whenever(
                statsRepository.fetchDevicesPlatform(any(), any())
            ).thenReturn(
                DevicesResult.Error(R.string.stats_error_api)
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.onDeviceTypeChanged(DeviceType.PLATFORM)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Error::class.java
            )
        }

    @Test
    fun `when repository throws exception, then unknown error state is emitted`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenThrow(RuntimeException("Network error"))

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Error::class.java
            )
            assertThat(
                (state as DevicesCardUiState.Error).messageResId
            ).isEqualTo(R.string.stats_error_unknown)
            assertThat(state.isAuthError).isFalse()
        }
    // endregion

    // region Success states
    @Test
    fun `when data loads successfully, then loaded state is emitted`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                DevicesCardUiState.Loaded::class.java
            )
        }

    @Test
    fun `when data loads, then items have correct values`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(createScreensizeSuccess())

        initViewModel()
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as DevicesCardUiState.Loaded
        assertThat(state.items).hasSize(3)
        assertThat(state.items[0].name)
            .isEqualTo(TEST_DEVICE_NAME_1)
        assertThat(state.items[0].value)
            .isEqualTo(TEST_DEVICE_VIEWS_1)
        assertThat(state.items[1].name)
            .isEqualTo(TEST_DEVICE_NAME_2)
        assertThat(state.items[1].value)
            .isEqualTo(TEST_DEVICE_VIEWS_2)
    }

    @Test
    fun `when data loads, then maxValueForBar is set to first item views`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as DevicesCardUiState.Loaded
            assertThat(state.maxValueForBar)
                .isEqualTo(TEST_DEVICE_VIEWS_1)
        }

    @Test
    fun `when data has zero-value items, then they are included`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(
                    any(), any()
                )
            ).thenReturn(
                DevicesResult.Success(
                    items = listOf(
                        DeviceItemData("Desktop", 57.6),
                        DeviceItemData("Mobile", 23.9),
                        DeviceItemData("Tablet", 0.0)
                    )
                )
            )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as DevicesCardUiState.Loaded
            assertThat(state.items).hasSize(3)
            assertThat(state.items[2].name)
                .isEqualTo("Tablet")
            assertThat(state.items[2].value).isEqualTo(0.0)
        }

    @Test
    fun `when data has zero-value items, then maxValueForBar ignores zeros`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(
                    any(), any()
                )
            ).thenReturn(
                DevicesResult.Success(
                    items = listOf(
                        DeviceItemData("Desktop", 57.6),
                        DeviceItemData("Tablet", 0.0)
                    )
                )
            )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as DevicesCardUiState.Loaded
            assertThat(state.maxValueForBar).isEqualTo(57.6)
        }

    @Test
    fun `when data loads with empty items, then loaded state with empty list is emitted`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(
                DevicesResult.Success(items = emptyList())
            )

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as DevicesCardUiState.Loaded
            assertThat(state.items).isEmpty()
            assertThat(state.maxValueForBar).isEqualTo(0.0)
        }
    // endregion

    // region Period changes
    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(createScreensizeSuccess())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchDevicesScreensize(any(), any())
        verify(statsRepository).fetchDevicesScreensize(
            eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days)
        )
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            viewModel.onPeriodChanged(StatsPeriod.Last7Days)
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchDevicesScreensize(any(), any())
        }
    // endregion

    // region Device type switching
    @Test
    fun `when switching to browser, then browser data is fetched`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())
            whenever(
                statsRepository.fetchDevicesBrowser(any(), any())
            ).thenReturn(createBrowserSuccess())

            initViewModel()
            advanceUntilIdle()

            viewModel.onDeviceTypeChanged(DeviceType.BROWSER)
            advanceUntilIdle()

            verify(statsRepository).fetchDevicesBrowser(
                eq(TEST_SITE_ID), any()
            )
        }

    @Test
    fun `when switching to same type, then data is not re-fetched`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            viewModel.onDeviceTypeChanged(DeviceType.SCREENSIZE)
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchDevicesScreensize(any(), any())
        }

    @Test
    fun `when switching to platform, then platform data is fetched`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())
            whenever(
                statsRepository.fetchDevicesPlatform(any(), any())
            ).thenReturn(createPlatformSuccess())

            initViewModel()
            advanceUntilIdle()

            viewModel.onDeviceTypeChanged(DeviceType.PLATFORM)
            advanceUntilIdle()

            verify(statsRepository).fetchDevicesPlatform(
                eq(TEST_SITE_ID), any()
            )
        }
    // endregion

    // region Refresh and Retry
    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()
        }

    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(createScreensizeSuccess())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchDevicesScreensize(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when refresh with no site, then data is not fetched`() =
        test {
            whenever(
                statsRepository.fetchDevicesScreensize(any(), any())
            ).thenReturn(createScreensizeSuccess())

            initViewModel()
            advanceUntilIdle()

            whenever(selectedSiteRepository.getSelectedSite())
                .thenReturn(null)

            viewModel.refresh()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchDevicesScreensize(any(), any())
        }

    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(
            statsRepository.fetchDevicesScreensize(any(), any())
        ).thenReturn(createScreensizeSuccess())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchDevicesScreensize(any(), any())
    }
    // endregion

    // region getAdminUrl
    @Test
    fun `when getAdminUrl called with site, then returns admin url`() =
        test {
            testSite.adminUrl = "https://example.com/wp-admin"

            initViewModel()

            assertThat(viewModel.getAdminUrl())
                .isEqualTo("https://example.com/wp-admin")
        }

    @Test
    fun `when getAdminUrl called without site, then returns null`() =
        test {
            whenever(selectedSiteRepository.getSelectedSite())
                .thenReturn(null)

            initViewModel()

            assertThat(viewModel.getAdminUrl()).isNull()
        }
    // endregion

    // region Helper functions
    private fun createScreensizeSuccess() = DevicesResult.Success(
        items = listOf(
            DeviceItemData(
                name = TEST_DEVICE_NAME_1,
                views = TEST_DEVICE_VIEWS_1
            ),
            DeviceItemData(
                name = TEST_DEVICE_NAME_2,
                views = TEST_DEVICE_VIEWS_2
            ),
            DeviceItemData(
                name = TEST_DEVICE_NAME_3,
                views = TEST_DEVICE_VIEWS_3
            )
        )
    )

    private fun createBrowserSuccess() = DevicesResult.Success(
        items = listOf(
            DeviceItemData(name = "Chrome", views = 400.0),
            DeviceItemData(name = "Safari", views = 200.0)
        )
    )

    private fun createPlatformSuccess() = DevicesResult.Success(
        items = listOf(
            DeviceItemData(name = "Windows", views = 350.0),
            DeviceItemData(name = "macOS", views = 250.0)
        )
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"

        private const val TEST_DEVICE_NAME_1 = "Desktop"
        private const val TEST_DEVICE_NAME_2 = "Mobile"
        private const val TEST_DEVICE_NAME_3 = "Tablet"
        private const val TEST_DEVICE_VIEWS_1 = 500.0
        private const val TEST_DEVICE_VIEWS_2 = 300.0
        private const val TEST_DEVICE_VIEWS_3 = 100.0
    }
}
