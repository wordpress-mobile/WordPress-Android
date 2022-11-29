package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ActivityLogAction.BACKUP_DOWNLOAD
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadErrorType.API_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownload
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
class PostBackupDownloadUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: PostBackupDownloadUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock lateinit var siteModel: SiteModel

    @Before
    fun setup() = test {
        useCase = PostBackupDownloadUseCase(networkUtilsWrapper, activityLogStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when download is triggered, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `given invalid response, when download is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId, BackupDownloadError(
                        INVALID_RESPONSE
                ), BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given generic error response, when download is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId, BackupDownloadError(
                        GENERIC_ERROR
                ), BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given api error response, when download is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId, BackupDownloadError(
                        API_ERROR
                ), BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `when download is triggered successfully, then Success is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId = rewindId,
                        downloadId = downloadId,
                        causeOfChange = BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(Success(requestRewindId = rewindId, rewindId = rewindId, downloadId = downloadId))
    }

    @Test
    fun `given download success, when downloadId is null, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId = rewindId,
                        downloadId = null,
                        causeOfChange = BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given download success, when unmatched rewindIds, then OtherRequestRunning is returned`() = test {
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId = "unmatchedRewindId",
                        downloadId = downloadId,
                        causeOfChange = BACKUP_DOWNLOAD
                )
        )

        val result = useCase.postBackupDownloadRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(OtherRequestRunning)
    }

    private val rewindId = "rewindId"
    private val downloadId = 100L

    private val types: BackupDownloadRequestTypes = BackupDownloadRequestTypes(
            themes = true,
            plugins = true,
            uploads = true,
            sqls = true,
            roots = true,
            contents = true
    )
}
