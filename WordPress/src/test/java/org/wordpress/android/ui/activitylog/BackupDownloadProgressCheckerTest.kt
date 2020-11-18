package org.wordpress.android.ui.activitylog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class BackupDownloadProgressCheckerTest {
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock lateinit var site: SiteModel
    private val restoreId = 1L
    private lateinit var backupDownloadProgressChecker: BackupDownloadProgressChecker

    @Before
    fun setUp() {
        backupDownloadProgressChecker = BackupDownloadProgressChecker(activityLogStore, TEST_SCOPE)
    }

    private val finishedBackupDownload = BackupDownloadStatusModel(
            downloadId = 1L,
            rewindId = "rewindId",
            backupPoint = Date(),
            startedAt = Date(),
            progress = null,
            downloadCount = 0,
            validUntil = Date(),
            url = "url"
    )

    private val backupDownloadStatusModel = BackupDownloadStatusModel(
            downloadId = 1L,
            rewindId = "rewindId",
            backupPoint = Date(),
            startedAt = Date(),
            progress = 35,
            downloadCount = null,
            validUntil = null,
            url = null
    )
    private val finishedBackupDownloadStatus = finishedBackupDownload.copy()

    @Test
    fun `on start checks current value and finishes if backup download is done`() = runBlocking {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                finishedBackupDownloadStatus
        )

        val onBackupDownloadStatusFetched = backupDownloadProgressChecker.start(
                site,
                restoreId,
                checkDelay = -1
        )

        assertEquals(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE),
                onBackupDownloadStatusFetched
        )
    }

    @Test
    fun `on start triggers fetch if backup dwnload in progress`() = runBlocking {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                backupDownloadStatusModel,
                finishedBackupDownloadStatus
        )
        whenever(activityLogStore.fetchActivitiesBackupDownload(any())).thenReturn(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
        )

        val onBackupDownloadStatusFetched = backupDownloadProgressChecker.start(
                site,
                restoreId,
                checkDelay = -1
        )

        with(inOrder(activityLogStore)) {
            verify(activityLogStore).getBackupDownloadStatusForSite(site)
            verify(activityLogStore).fetchActivitiesBackupDownload(any())
            verify(activityLogStore).getBackupDownloadStatusForSite(site)
        }

        assertEquals(
                OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE),
                onBackupDownloadStatusFetched
        )
    }

    @Test
    fun `on fetch fail return error`() = runBlocking {
        whenever(activityLogStore.getBackupDownloadStatusForSite(site)).thenReturn(
                backupDownloadStatusModel
        )
        val errorStatus = OnBackupDownloadStatusFetched(
                BackupDownloadStatusError(
                        BackupDownloadStatusErrorType.GENERIC_ERROR,
                        "generic error"
                ), FETCH_BACKUP_DOWNLOAD_STATE
        )
        whenever(activityLogStore.fetchActivitiesBackupDownload(any())).thenReturn(errorStatus)

        val onBackupDownloadStatusFetched = backupDownloadProgressChecker.start(
                site,
                restoreId,
                checkDelay = -1
        )

        with(inOrder(activityLogStore)) {
            verify(activityLogStore).getBackupDownloadStatusForSite(site)
            verify(activityLogStore).fetchActivitiesBackupDownload(any())
            verifyNoMoreInteractions()
        }

        assertEquals(errorStatus, onBackupDownloadStatusFetched)
    }

    @Test
    fun `on cancel stops a job`() = runBlocking {
        val onBackupDownloadStatusFetched = launch {
            backupDownloadProgressChecker.start(site, restoreId)
        }

        onBackupDownloadStatusFetched.cancel()

        verifyZeroInteractions(activityLogStore)
    }
}
