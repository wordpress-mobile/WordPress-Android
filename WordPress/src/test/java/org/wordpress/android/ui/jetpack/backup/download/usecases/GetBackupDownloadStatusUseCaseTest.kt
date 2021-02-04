package org.wordpress.android.ui.jetpack.backup.download.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Empty
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class GetBackupDownloadStatusUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: GetBackupDownloadStatusUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var site: SiteModel

    @ExperimentalCoroutinesApi
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    @Before
    fun setup() = test {
        useCase = GetBackupDownloadStatusUseCase(networkUtilsWrapper, activityLogStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(activityLogStore.fetchBackupDownloadState(any()))
                .thenReturn(OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE))
        whenever(activityLogStore.getActivityLogItemByRewindId(rewindId)).thenReturn(activityLogModel)
    }

    @Test
    fun `given no network without download id, when backup status triggers, then return network unavailable`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.getBackupDownloadStatus(site, null).toList()

        assertThat(result).contains(Failure.NetworkUnavailable)
    }

    @Test
    fun `given no network with download id, when backup status triggers, then return network unavailable`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.getBackupDownloadStatus(site, downloadId).toList()

        assertThat(result).contains(Failure.NetworkUnavailable)
    }

    @Test
    fun `given failure without download id, when backup status triggers, then return remote request failure`() =
            coroutineScope.runBlockingTest {
                whenever(activityLogStore.fetchBackupDownloadState(any()))
                        .thenReturn(
                                OnBackupDownloadStatusFetched(
                                        BackupDownloadStatusError(GENERIC_ERROR),
                                        FETCH_BACKUP_DOWNLOAD_STATE
                                )
                        )

                val result = useCase.getBackupDownloadStatus(site, null).toList()
                advanceTimeBy(DELAY_MILLIS)

                assertThat(result).contains(RemoteRequestFailure)
            }

    @Test
    fun `given failure with download id, when backup status triggers, then return remote request failure`() =
            coroutineScope.runBlockingTest {
                whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                        OnBackupDownloadStatusFetched(
                                BackupDownloadStatusError(GENERIC_ERROR),
                                FETCH_BACKUP_DOWNLOAD_STATE
                        )
                )

                val result = useCase.getBackupDownloadStatus(site, downloadId).toList()
                advanceTimeBy(DELAY_MILLIS)

                assertThat(result).contains(RemoteRequestFailure)
            }

    @Test
    fun `given success without download id, when backup status triggers, then return complete`() = test {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(statusModel)

        val result = useCase.getBackupDownloadStatus(site, null).toList()

        assertThat(result).contains(completeStatus)
    }

    @Test
    fun `given success with download id, when backup status triggers, then return complete`() = test {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(statusModel)

        val result = useCase.getBackupDownloadStatus(site, downloadId).toList()

        assertThat(result).contains(completeStatus)
    }

    @Test
    fun `given status model is null without download id, when backup status triggers, then return empty`() = test {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(null)

        val result = useCase.getBackupDownloadStatus(site, null).toList()

        assertThat(result).contains(Empty)
    }

    @Test
    fun `given status model is null with download id, when backup status triggers, then return empty`() = test {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(null)

        val result = useCase.getBackupDownloadStatus(site, downloadId).toList()

        assertThat(result).contains(Empty)
    }

    @Test
    fun `given download in process without download id, when backup status triggers, then return progress`() =
            coroutineScope.runBlockingTest {
                whenever(activityLogStore.getBackupDownloadStatusForSite(site))
                        .thenReturn(inProgressModel)
                        .thenReturn(statusModel)
                whenever(activityLogStore.fetchBackupDownloadState(any()))
                        .thenReturn(OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE))

                val result = useCase.getBackupDownloadStatus(site, null).toList()
                advanceTimeBy(DELAY_MILLIS)

                assertThat(result).contains(progressStatus, completeStatus)
            }

    @Test
    fun `given download in process with download id, when backup status triggers, then return progress`() =
            coroutineScope.runBlockingTest {
                whenever(activityLogStore.getBackupDownloadStatusForSite(site))
                        .thenReturn(inProgressModel)
                        .thenReturn(statusModel)
                whenever(activityLogStore.fetchBackupDownloadState(any()))
                        .thenReturn(OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE))

                val result = useCase.getBackupDownloadStatus(site, downloadId).toList()
                advanceTimeBy(DELAY_MILLIS)

                assertThat(result).contains(progressStatus, completeStatus)
            }

    private val rewindId = "rewindId"
    private val url = "www.wordpress.com"
    private val downloadId = 100L
    private val progress = 50
    private val published = Date()

    private val statusModel = BackupDownloadStatusModel(
            downloadId = downloadId,
            rewindId = rewindId,
            backupPoint = Date(1609690147756),
            startedAt = Date(1609690147756),
            progress = null,
            downloadCount = 0,
            validUntil = Date(1609690147756),
            url = url
    )

    private val inProgressModel = BackupDownloadStatusModel(
            downloadId = downloadId,
            rewindId = rewindId,
            backupPoint = Date(1609690147756),
            startedAt = Date(1609690147756),
            progress = progress,
            downloadCount = 0,
            validUntil = Date(1609690147756),
            url = url
    )

    private val activityLogModel = ActivityLogModel(
            activityID = "activityID",
            summary = "summary",
            content = null,
            name = null,
            type = null,
            gridicon = null,
            status = null,
            rewindable = null,
            rewindID = null,
            published = published,
            actor = null
    )

    private val completeStatus = Complete(rewindId, downloadId, url, published)
    private val progressStatus = Progress(rewindId, progress, published)
}
