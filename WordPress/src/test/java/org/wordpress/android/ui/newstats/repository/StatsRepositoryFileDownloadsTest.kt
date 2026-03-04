package org.wordpress.android.ui.newstats.repository

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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.FileDownloadDataItem
import org.wordpress.android.ui.newstats.datasource.FileDownloadsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType

@ExperimentalCoroutinesApi
class StatsRepositoryFileDownloadsTest : BaseUnitTest() {
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

    @Test
    fun `given successful response, when fetchFileDownloads, then success result is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(
                    createFileDownloadItems()
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name)
                .isEqualTo(TEST_FILE_NAME_1)
            assertThat(success.items[0].downloads)
                .isEqualTo(TEST_FILE_DOWNLOADS_1)
            assertThat(success.items[1].name)
                .isEqualTo(TEST_FILE_NAME_2)
            assertThat(success.items[1].downloads)
                .isEqualTo(TEST_FILE_DOWNLOADS_2)
        }

    @Test
    fun `given successful response, when fetchFileDownloads, then totalDownloads is sum of item downloads`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(
                    createFileDownloadItems()
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.totalDownloads).isEqualTo(
                TEST_FILE_DOWNLOADS_1 + TEST_FILE_DOWNLOADS_2
            )
        }

    @Test
    fun `given current and previous data, when fetchFileDownloads, then change is calculated correctly`() =
        test {
            val currentItems = listOf(
                FileDownloadDataItem("file1.pdf", 150),
                FileDownloadDataItem("file2.zip", 100)
            )
            val previousItems = listOf(
                FileDownloadDataItem("file1.pdf", 100),
                FileDownloadDataItem("file2.zip", 100)
            )

            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(currentItems)
            ).thenReturn(
                FileDownloadsDataResult.Success(previousItems)
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            // Current total: 250, Previous total: 200, Change: 50
            assertThat(success.totalDownloads).isEqualTo(250)
            assertThat(success.totalDownloadsChange).isEqualTo(50)
            assertThat(success.totalDownloadsChangePercent)
                .isEqualTo(25.0)
        }

    @Test
    fun `given item in both periods, when fetchFileDownloads, then previousDownloads is set correctly`() =
        test {
            val currentItems = listOf(
                FileDownloadDataItem("file1.pdf", 150)
            )
            val previousItems = listOf(
                FileDownloadDataItem("file1.pdf", 100)
            )

            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(currentItems)
            ).thenReturn(
                FileDownloadsDataResult.Success(previousItems)
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.items[0].previousDownloads)
                .isEqualTo(100)
            assertThat(success.items[0].downloadsChange)
                .isEqualTo(50)
            assertThat(success.items[0].downloadsChangePercent)
                .isEqualTo(50.0)
        }

    @Test
    fun `given new item not in previous period, when fetchFileDownloads, then previousDownloads is zero`() =
        test {
            val currentItems = listOf(
                FileDownloadDataItem("new-file.pdf", 100)
            )
            val previousItems =
                emptyList<FileDownloadDataItem>()

            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(currentItems)
            ).thenReturn(
                FileDownloadsDataResult.Success(previousItems)
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.items[0].previousDownloads)
                .isEqualTo(0)
            assertThat(success.items[0].downloadsChange)
                .isEqualTo(100)
            assertThat(success.items[0].downloadsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given previous fetch fails, when fetchFileDownloads, then previousDownloads defaults to zero`() =
        test {
            val currentItems = listOf(
                FileDownloadDataItem("file1.pdf", 100)
            )

            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(currentItems)
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.items[0].previousDownloads)
                .isEqualTo(0)
            assertThat(success.totalDownloadsChange)
                .isEqualTo(100)
            assertThat(success.totalDownloadsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given error response, when fetchFileDownloads, then error result is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            assertThat(
                (result as FileDownloadsResult.Error)
                    .messageResId
            ).isEqualTo(R.string.stats_error_network)
        }

    @Test
    fun `given auth error, when fetchFileDownloads, then isAuthError is true`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.AUTH_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            val error = result as FileDownloadsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_auth)
            assertThat(error.isAuthError).isTrue()
        }

    @Test
    fun `given non-auth error, when fetchFileDownloads, then isAuthError is false`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            assertThat(
                (result as FileDownloadsResult.Error)
                    .isAuthError
            ).isFalse()
        }

    @Test
    fun `given parsing error, when fetchFileDownloads, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.PARSING_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            val error = result as FileDownloadsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_parsing)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given api error, when fetchFileDownloads, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.API_ERROR
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            val error = result as FileDownloadsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given unknown error, when fetchFileDownloads, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Error(
                    StatsErrorType.UNKNOWN
                )
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Error::class.java
            )
            val error = result as FileDownloadsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_unknown)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `when fetchFileDownloads is called, then data source is called twice for comparison`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(
                    createFileDownloadItems()
                )
            )

            repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            verify(
                statsDataSource, times(2)
            ).fetchFileDownloads(
                siteId = eq(TEST_SITE_ID),
                dateRange = any(),
                max = eq(0)
            )
        }

    @Test
    fun `given empty items list, when fetchFileDownloads, then success with empty list is returned`() =
        test {
            whenever(
                statsDataSource.fetchFileDownloads(
                    any(), any(), any()
                )
            ).thenReturn(
                FileDownloadsDataResult.Success(emptyList())
            )

            val result = repository.fetchFileDownloads(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result).isInstanceOf(
                FileDownloadsResult.Success::class.java
            )
            val success = result as FileDownloadsResult.Success
            assertThat(success.items).isEmpty()
            assertThat(success.totalDownloads).isEqualTo(0)
            assertThat(success.totalDownloadsChange).isEqualTo(0)
            assertThat(success.totalDownloadsChangePercent)
                .isEqualTo(0.0)
        }

    private fun createFileDownloadItems() = listOf(
        FileDownloadDataItem(
            name = TEST_FILE_NAME_1,
            downloads = TEST_FILE_DOWNLOADS_1
        ),
        FileDownloadDataItem(
            name = TEST_FILE_NAME_2,
            downloads = TEST_FILE_DOWNLOADS_2
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L

        private const val TEST_FILE_NAME_1 = "report.pdf"
        private const val TEST_FILE_NAME_2 = "archive.zip"
        private const val TEST_FILE_DOWNLOADS_1 = 500L
        private const val TEST_FILE_DOWNLOADS_2 = 300L
    }
}
