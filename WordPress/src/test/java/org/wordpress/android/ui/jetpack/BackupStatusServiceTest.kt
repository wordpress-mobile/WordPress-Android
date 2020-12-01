package org.wordpress.android.ui.jetpack

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.action.ActivityLogAction.BACKUP_DOWNLOAD
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.jetpack.backup.BackupStatusService.BackupDownloadProgress
import org.wordpress.android.ui.jetpack.backup.BackupDownloadProgressChecker
import org.wordpress.android.ui.jetpack.backup.BackupStatusService
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class BackupStatusServiceTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    private val backupDownloadStatusCaptor = argumentCaptor<FetchBackupDownloadStatePayload>()
    private val backupDownloadCaptor = argumentCaptor<BackupDownloadPayload>()

    @Mock private lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var backupDownloadProgressChecker: BackupDownloadProgressChecker
    @Mock private lateinit var site: SiteModel

    private lateinit var mBackupStatusService: BackupStatusService
    private var backupDownloadAvailable: Boolean? = null
    private var backupDownloadProgress: BackupDownloadProgress? = null
    private var backupDownloadError: BackupDownloadError? = null
    private var backupDownloadStatusFetchError: BackupDownloadStatusError? = null

    private val rewindId = "10"
    private val downloadId = 10L
    private val activityID = "activityId"
    private val progress = 35
    private val backupPoint = Date()
    private val startedAt = Date()
    private val published = Date()
    private val activityLogModel = ActivityLogModel(
            activityID,
            "summary",
            FormattableContent(text = "text"),
            null,
            null,
            null,
            null,
            null,
            rewindId,
            published
    )

    private val inProgressBackupDownloadStatusModel = BackupDownloadStatusModel(
            downloadId = downloadId,
            rewindId = rewindId,
            backupPoint = backupPoint,
            startedAt = startedAt,
            progress = progress,
            downloadCount = null,
            validUntil = null,
            url = null
    )

    private val types = BackupDownloadRequestTypes(
            themes = true,
            plugins = true,
            uploads = true,
            sqls = true,
            roots = true,
            contents = true
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        mBackupStatusService = BackupStatusService(
                activityLogStore,
                backupDownloadProgressChecker,
                TEST_SCOPE
        )
        backupDownloadAvailable = null
        mBackupStatusService.backupDownloadAvailable.observeForever { backupDownloadAvailable = it }
        mBackupStatusService.backupDownloadProgress.observeForever { backupDownloadProgress = it }
        mBackupStatusService.backupDownloadError.observeForever { backupDownloadError = it }
        mBackupStatusService.backupDownloadStatusFetchError.observeForever {
            backupDownloadStatusFetchError = it }
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(null)
        whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
        )
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId = rewindId,
                        causeOfChange = BACKUP_DOWNLOAD
                )
        )
        whenever(activityLogStore.getActivityLogItemByRewindId(rewindId)).thenReturn(
                activityLogModel
        )
        whenever(backupDownloadProgressChecker.startNow(any(), any())).thenReturn(null)
    }

    @After
    fun tearDown() {
        mBackupStatusService.stop()
    }

    @Test
    fun `emits available BackupDownloadStatus on start when download not in progress`() {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                inProgressBackupDownloadStatusModel.copy(progress = null)
        )

        mBackupStatusService.start(site)

        assertEquals(backupDownloadAvailable, true)
    }

    @Test
    fun `emits unavailable BackupDownloadStatus on start when download is in progress`() = runBlocking {
        val inactiveBackupDownloadStatusModel = inProgressBackupDownloadStatusModel
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                inactiveBackupDownloadStatusModel,
                null
        )

        mBackupStatusService.start(site)

        assertEquals(backupDownloadAvailable, false)
    }

    @Test
    fun `triggers fetch when BackupDownloadStatus not available`() = runBlocking {
        mBackupStatusService.start(site)

        assertFetchBackupDownloadStatusAction()
    }

    @Test
    fun `updates BackupDownloadStatus and restarts checker when BackupDownload not already running`() =
            runBlocking<Unit> {
                mBackupStatusService.start(site)
                whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                        inProgressBackupDownloadStatusModel
                )
                whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                        OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
                )
                reset(backupDownloadProgressChecker)

                mBackupStatusService.requestStatusUpdate()

                verify(backupDownloadProgressChecker).startNow(
                        site,
                        inProgressBackupDownloadStatusModel.downloadId
                )
            }

    @Test
    fun `triggers BackupDownload and makes action unavailable`() = runBlocking {
        val rewindId = "10"

        mBackupStatusService.backupDownload(rewindId, site, types)

        assertBackupDownloadAction(rewindId)
        assertEquals(false, backupDownloadAvailable)
        assertEquals(backupDownloadProgress, BackupDownloadProgress(activityLogModel, 0))
    }

    @Test
    fun `cancels worker OnFetchErrorBackupDownloadState and emits error`() = runBlocking {
        mBackupStatusService.start(site)
        val error = BackupDownloadStatusError(BackupDownloadStatusErrorType.INVALID_RESPONSE, null)
        whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                OnBackupDownloadStatusFetched(error, BACKUP_DOWNLOAD)
        )

        mBackupStatusService.requestStatusUpdate()

        assertEquals(error, backupDownloadStatusFetchError)
    }

    @Test
    fun `when onBackupDownloadState in progress update state`() = runBlocking {
        mBackupStatusService.start(site)

        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                inProgressBackupDownloadStatusModel
        )
        whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
        )

        mBackupStatusService.requestStatusUpdate()

        assertEquals(backupDownloadAvailable, false)
        assertEquals(backupDownloadProgress, BackupDownloadProgress(activityLogModel, progress))
    }

    @Test
    fun `when onBackupDownloadState finished update state`() = runBlocking {
        mBackupStatusService.start(site)

        val backupDownloadFinished = inProgressBackupDownloadStatusModel.copy(progress = null)

        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                inProgressBackupDownloadStatusModel,
                backupDownloadFinished
        )

        whenever(activityLogStore.fetchBackupDownloadState(any())).thenReturn(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
        )

        mBackupStatusService.requestStatusUpdate()

        assertEquals(backupDownloadAvailable, true)
        assertEquals(backupDownloadProgress?.progress, null)
    }

    @Test
    fun `when onBackupDownload error cancel worker and re-enable BackupDownloadStatus`() = runBlocking {
        mBackupStatusService.start(site)

        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                inProgressBackupDownloadStatusModel
        )

        val error = BackupDownloadError(BackupDownloadErrorType.INVALID_RESPONSE, null)
        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId,
                        error,
                        BACKUP_DOWNLOAD
                )
        )

        backupDownloadAvailable = null

        mBackupStatusService.backupDownload(rewindId, site, types)

        assertEquals(backupDownloadAvailable, false)
        assertEquals(error, backupDownloadError)
        val progress = BackupDownloadProgress(activityLogModel, progress)
        assertEquals(backupDownloadProgress, progress)
    }

    @Test
    fun `onBackupDownloadFetchStatus start worker`() = runBlocking<Unit> {
        mBackupStatusService.start(site)
        reset(backupDownloadProgressChecker)

        whenever(activityLogStore.backupDownload(any())).thenReturn(
                OnBackupDownload(
                        rewindId = rewindId,
                        downloadId = downloadId,
                        causeOfChange = BACKUP_DOWNLOAD
                )
        )

        mBackupStatusService.backupDownload(rewindId, site, types)

        verify(backupDownloadProgressChecker).start(site, downloadId)
    }

    private suspend fun assertFetchBackupDownloadStatusAction() {
        verify(activityLogStore).fetchBackupDownloadState(backupDownloadStatusCaptor.capture())
        backupDownloadStatusCaptor.firstValue.apply {
            assertEquals(this.site, site)
        }
    }

    private suspend fun assertBackupDownloadAction(rewindId: String) {
        verify(activityLogStore).backupDownload(backupDownloadCaptor.capture())
        backupDownloadCaptor.firstValue.apply {
            assertEquals(this.site, site)
            assertEquals(this.rewindId, rewindId)
        }
    }
}
