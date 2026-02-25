package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.DevicesData
import org.wordpress.android.ui.newstats.datasource.DevicesDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType

@ExperimentalCoroutinesApi
class StatsRepositoryDevicesTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher()
        )
    }

    // region Screensize
    @Test
    fun `given success, when fetchDevicesScreensize, then items are returned sorted by views`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Mobile" to 200.0,
                            "Desktop" to 500.0,
                            "Tablet" to 100.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(DevicesResult.Success::class.java)
            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(3)
            assertThat(success.items[0].name).isEqualTo("Desktop")
            assertThat(success.items[0].views).isEqualTo(500.0)
            assertThat(success.items[1].name).isEqualTo("Mobile")
            assertThat(success.items[2].name).isEqualTo("Tablet")
        }

    @Test
    fun `given items with zero views, when fetchDevicesScreensize, then zero-view items are included`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Desktop" to 500.0,
                            "Mobile" to 0.0,
                            "Tablet" to 100.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(3)
            assertThat(success.items.map { it.name })
                .containsExactly("Desktop", "Tablet", "Mobile")
        }

    @Test
    fun `given all items zero views, when fetchDevicesScreensize, then items are still returned`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Desktop" to 0.0,
                            "Mobile" to 0.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(2)
        }

    @Test
    fun `given empty response, when fetchDevicesScreensize, then empty list is returned`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(items = emptyMap())
                )
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).isEmpty()
        }

    @Test
    fun `given decimal values, when fetchDevicesScreensize, then values are preserved`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Desktop" to 57.6,
                            "Mobile" to 23.9,
                            "Tablet" to 0.5
                        )
                    )
                )
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(3)
            assertThat(success.items[0].views)
                .isEqualTo(57.6)
            assertThat(success.items[1].views)
                .isEqualTo(23.9)
            assertThat(success.items[2].views)
                .isEqualTo(0.5)
        }

    @Test
    fun `given error, when fetchDevicesScreensize, then error result is returned`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Error(StatsErrorType.API_ERROR)
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(DevicesResult.Error::class.java)
            val error = result as DevicesResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `given auth error, when fetchDevicesScreensize, then isAuthError is true`() =
        test {
            whenever(
                statsDataSource.fetchDevicesScreensize(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Error(StatsErrorType.AUTH_ERROR)
            )

            val result = repository.fetchDevicesScreensize(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val error = result as DevicesResult.Error
            assertThat(error.isAuthError).isTrue()
        }
    // endregion

    // region Browser
    @Test
    fun `given success, when fetchDevicesBrowser, then items are returned`() =
        test {
            whenever(
                statsDataSource.fetchDevicesBrowser(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Chrome" to 400.0,
                            "Safari" to 200.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesBrowser(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name).isEqualTo("Chrome")
        }

    @Test
    fun `given items with zero views, when fetchDevicesBrowser, then zero-view items are included`() =
        test {
            whenever(
                statsDataSource.fetchDevicesBrowser(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Chrome" to 400.0,
                            "IE" to 0.0,
                            "Safari" to 200.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesBrowser(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(3)
            assertThat(success.items.map { it.name })
                .containsExactly("Chrome", "Safari", "IE")
        }
    // endregion

    // region Platform
    @Test
    fun `given success, when fetchDevicesPlatform, then items are returned`() =
        test {
            whenever(
                statsDataSource.fetchDevicesPlatform(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Windows" to 350.0,
                            "macOS" to 250.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesPlatform(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name).isEqualTo("Windows")
        }

    @Test
    fun `given items with zero views, when fetchDevicesPlatform, then zero-view items are included`() =
        test {
            whenever(
                statsDataSource.fetchDevicesPlatform(
                    any(), any(), any()
                )
            ).thenReturn(
                DevicesDataResult.Success(
                    DevicesData(
                        items = mapOf(
                            "Windows" to 350.0,
                            "Linux" to 0.0
                        )
                    )
                )
            )

            val result = repository.fetchDevicesPlatform(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            val success = result as DevicesResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name).isEqualTo("Windows")
        }
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
