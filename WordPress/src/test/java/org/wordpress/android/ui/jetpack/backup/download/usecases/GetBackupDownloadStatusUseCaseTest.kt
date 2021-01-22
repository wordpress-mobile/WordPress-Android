package org.wordpress.android.ui.jetpack.backup.download.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType.GENERIC_ERROR
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Empty
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import java.util.Date

@InternalCoroutinesApi
class GetBackupDownloadStatusUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: GetBackupDownloadStatusUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock lateinit var statusModel: BackupDownloadStatusModel

    @Before
    fun setup() {
        useCase = GetBackupDownloadStatusUseCase(networkUtilsWrapper, activityLogStore)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(statusModel)
    }

    @Test
    fun `given site, when status is fetched, then NetworkUnavailable is returned on no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.getBackupDownloadStatus(site, downloadId).toList(mutableListOf())

        Assertions.assertThat(result).contains(Failure.NetworkUnavailable)
    }

    @Test
    fun `given site, when status is fetched, then RemoteRequestFailure is returned on failure`() = test {
        whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                OnBackupDownloadStatusFetched(BackupDownloadStatusError(GENERIC_ERROR), FETCH_BACKUP_DOWNLOAD_STATE)
        )

        val result = useCase.getBackupDownloadStatus(site, downloadId).toList(mutableListOf())

        Assertions.assertThat(result).contains(RemoteRequestFailure)
    }

    @Test
    fun `given site, when status is fetched, then Complete is returned on success`() = testWithSuccessResponse {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(completeStateModel)
        val result = useCase.getBackupDownloadStatus(site, downloadId).toList(mutableListOf())

        Assertions.assertThat(result).contains(completeStatus)
    }

    @Test
    fun `given site, when status is fetched, then Empty is returned when empty`() = testWithSuccessResponse {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(null)
        val result = useCase.getBackupDownloadStatus(site, downloadId).toList(mutableListOf())

        Assertions.assertThat(result).contains(Empty)
    }

    @Test
    fun `given state, when backup download is running, then Progress is returned`() = testWithSuccessResponse {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(null)
        val result = useCase.getBackupDownloadStatus(site, downloadId).toList(mutableListOf())

        Assertions.assertThat(result).contains(Empty)
    }

    private fun <T> testWithSuccessResponse(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                    OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
            )
            block()
        }
    }

    private val rewindId = "rewindId"
    private val url = "www.wordpress.com"
    private val downloadId = 100L

    private val completeStateModel = BackupDownloadStatusModel(
        downloadId = downloadId,
        rewindId = rewindId,
        backupPoint = Date(1609690147756),
        startedAt = Date(1609690147756),
        progress = null,
        downloadCount = 0,
        validUntil = Date(1609690147756)   ,
        url = url
    )

    private val completeStatus = Complete(rewindId, downloadId, url)
}
