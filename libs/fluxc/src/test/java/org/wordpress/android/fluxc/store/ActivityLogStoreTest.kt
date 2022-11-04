package org.wordpress.android.fluxc.store

import com.yarolegovich.wellsql.SelectQuery
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindResultPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ActivityLogStoreTest {
    @Mock private lateinit var activityLogRestClient: ActivityLogRestClient
    @Mock private lateinit var activityLogSqlUtils: ActivityLogSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var activityLogStore: ActivityLogStore

    @Before
    fun setUp() {
        activityLogStore = ActivityLogStore(activityLogRestClient, activityLogSqlUtils,
                initCoroutineEngine(), dispatcher)
    }

    @Test
    fun onFetchActivityLogFirstPageActionCleanupDbAndCallRestClient() = test {
        val payload = FetchActivityLogPayload(siteModel)
        whenever(activityLogRestClient.fetchActivity(eq(payload), any(), any())).thenReturn(
                FetchedActivityLogPayload(
                        listOf(),
                        siteModel,
                        0,
                        0,
                        0
                )
        )

        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(payload, PAGE_SIZE, OFFSET)
    }

    @Test
    fun onFetchActivityLogNextActionReadCurrentDataAndCallRestClient() = test {
        val payload = FetchActivityLogPayload(siteModel, loadMore = true)
        whenever(activityLogRestClient.fetchActivity(eq(payload), any(), any())).thenReturn(
                FetchedActivityLogPayload(
                        listOf(),
                        siteModel,
                        0,
                        0,
                        0
                )
        )

        val existingActivities = listOf<ActivityLogModel>(mock())
        whenever(activityLogSqlUtils.getActivitiesForSite(siteModel, SelectQuery.ORDER_ASCENDING))
                .thenReturn(existingActivities)

        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(payload, PAGE_SIZE, existingActivities.size)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() = test {
        val payload = FetchRewindStatePayload(siteModel)
        whenever(activityLogRestClient.fetchActivityRewind(siteModel)).thenReturn(
                FetchedRewindStatePayload(
                        null,
                        siteModel
                )
        )
        val action = ActivityLogActionBuilder.newFetchRewindStateAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivityRewind(siteModel)
    }

    @Test
    fun onRewindActionCallRestClient() = test {
        whenever(activityLogRestClient.rewind(eq(siteModel), any(), anyOrNull())).thenReturn(
                RewindResultPayload(
                        "rewindId",
                        null,
                        siteModel
                )
        )

        val rewindId = "rewindId"
        val payload = RewindPayload(siteModel, rewindId, null)
        val action = ActivityLogActionBuilder.newRewindAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).rewind(siteModel, rewindId)
    }

    @Test
    fun storeFetchedActivityLogToDbAndSetsLoadMoreToFalse() = test {
        val rowsAffected = 1
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected)

        activityLogStore.onAction(action)

        verify(activityLogSqlUtils).insertOrUpdateActivities(siteModel, activityModels)
        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(rowsAffected,
                false,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
        verify(activityLogSqlUtils).deleteActivityLog(siteModel)
    }

    @Test
    fun cannotLoadMoreWhenResponseEmpty() = test {
        val rowsAffected = 0
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected)

        activityLogStore.onAction(action)

        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(0,
                false,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun setsLoadMoreToTrueOnMoreItems() = test {
        val rowsAffected = 1
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected, totalItems = 500)
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)

        activityLogStore.onAction(action)

        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(rowsAffected,
                true,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnActivitiesFromDb() {
        val activityModels = listOf<ActivityLogModel>(mock())
        whenever(activityLogSqlUtils.getActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING))
                .thenReturn(activityModels)

        val activityModelsFromDb = activityLogStore.getActivityLogForSite(
                site = siteModel,
                ascending = false,
                rewindableOnly = false
        )

        verify(activityLogSqlUtils).getActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING)
        assertEquals(activityModels, activityModelsFromDb)
    }

    @Test
    fun returnRewindableOnlyActivitiesFromDb() {
        val rewindableActivityModels = listOf<ActivityLogModel>(mock())
        whenever(activityLogSqlUtils.getRewindableActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING))
                .thenReturn(rewindableActivityModels)

        val activityModelsFromDb = activityLogStore.getActivityLogForSite(
                site = siteModel,
                ascending = false,
                rewindableOnly = true
        )

        verify(activityLogSqlUtils).getRewindableActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING)
        assertEquals(rewindableActivityModels, activityModelsFromDb)
    }

    @Test
    fun storeFetchedRewindStatusToDb() = test {
        val rewindStatusModel = mock<RewindStatusModel>()
        val payload = FetchedRewindStatePayload(rewindStatusModel, siteModel)
        whenever(activityLogRestClient.fetchActivityRewind(siteModel)).thenReturn(payload)

        val fetchAction = ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(siteModel))
        activityLogStore.onAction(fetchAction)

        verify(activityLogSqlUtils).replaceRewindStatus(siteModel, rewindStatusModel)
        val expectedChangeEvent = ActivityLogStore.OnRewindStatusFetched(ActivityLogAction.FETCH_REWIND_STATE)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnRewindStatusFromDb() {
        val rewindStatusModel = mock<RewindStatusModel>()
        whenever(activityLogSqlUtils.getRewindStatusForSite(siteModel))
                .thenReturn(rewindStatusModel)

        val rewindStatusFromDb = activityLogStore.getRewindStatusForSite(siteModel)

        verify(activityLogSqlUtils).getRewindStatusForSite(siteModel)
        assertEquals(rewindStatusModel, rewindStatusFromDb)
    }

    @Test
    fun emitsRewindResult() = test {
        val rewindId = "rewindId"
        val restoreId = 10L

        val payload = RewindResultPayload(rewindId, restoreId, siteModel)
        whenever(activityLogRestClient.rewind(siteModel, rewindId)).thenReturn(payload)

        activityLogStore.onAction(ActivityLogActionBuilder.newRewindAction(RewindPayload(
                siteModel,
                rewindId,
                null)))

        val expectedChangeEvent = ActivityLogStore.OnRewind(rewindId, restoreId, ActivityLogAction.REWIND)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnsActivityLogItemFromDbByRewindId() {
        val rewindId = "rewindId"
        val activityLogModel = mock<ActivityLogModel>()
        whenever(activityLogSqlUtils.getActivityByRewindId(rewindId)).thenReturn(activityLogModel)

        val returnedItem = activityLogStore.getActivityLogItemByRewindId(rewindId)

        assertEquals(activityLogModel, returnedItem)
        verify(activityLogSqlUtils).getActivityByRewindId(rewindId)
    }

    @Test
    fun returnsActivityLogItemFromDbByActivityId() {
        val rewindId = "activityId"
        val activityLogModel = mock<ActivityLogModel>()
        whenever(activityLogSqlUtils.getActivityByActivityId(rewindId)).thenReturn(activityLogModel)

        val returnedItem = activityLogStore.getActivityLogItemByActivityId(rewindId)

        assertEquals(activityLogModel, returnedItem)
        verify(activityLogSqlUtils).getActivityByActivityId(rewindId)
    }

    @Test
    fun onRewindActionWithTypesCallRestClient() = test {
        whenever(activityLogRestClient.rewind(eq(siteModel), any(), any())).thenReturn(
                RewindResultPayload(
                        "rewindId",
                        null,
                        siteModel
                )
        )

        val rewindId = "rewindId"
        val types = RewindRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        val payload = RewindPayload(siteModel, rewindId, types)
        val action = ActivityLogActionBuilder.newRewindAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).rewind(siteModel, rewindId, types)
    }

    @Test
    fun emitsRewindResultWhenSendingTypes() = test {
        val rewindId = "rewindId"
        val restoreId = 10L
        val types = RewindRequestTypes(themes = true,
                    plugins = true,
                    uploads = true,
                    sqls = true,
                    roots = true,
                    contents = true)

        val payload = RewindResultPayload(rewindId, restoreId, siteModel)
        whenever(activityLogRestClient.rewind(siteModel, rewindId, types)).thenReturn(payload)

        activityLogStore.onAction(ActivityLogActionBuilder.newRewindAction(RewindPayload(siteModel, rewindId, types)))

        val expectedChangeEvent = ActivityLogStore.OnRewind(rewindId, restoreId, ActivityLogAction.REWIND)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun onBackupDownloadActionCallRestClient() = test {
        whenever(activityLogRestClient.backupDownload(eq(siteModel), any(), any())).thenReturn(
                BackupDownloadResultPayload(
                        "rewindId",
                        10L,
                        "backupPoint",
                        "startedAt",
                        50,
                        siteModel
                )
        )

        val types = BackupDownloadRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        val rewindId = "rewindId"
        val payload = BackupDownloadPayload(siteModel, rewindId, types)
        val action = ActivityLogActionBuilder.newBackupDownloadAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).backupDownload(siteModel, rewindId, types)
    }

    @Test
    fun emitsBackupDownloadResult() = test {
        val rewindId = "rewindId"
        val downloadId = 10L
        val backupPoint = "backup_point"
        val startedAt = "started_at"
        val progress = 50
        val types = BackupDownloadRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)

        val payload = BackupDownloadResultPayload(
                rewindId,
                downloadId,
                backupPoint,
                startedAt,
                progress,
                siteModel)
        whenever(activityLogRestClient.backupDownload(siteModel, rewindId, types)).thenReturn(payload)

        activityLogStore.onAction(ActivityLogActionBuilder.newBackupDownloadAction(BackupDownloadPayload(
                siteModel,
                rewindId,
                types)))

        val expectedChangeEvent = ActivityLogStore.OnBackupDownload(
                rewindId,
                downloadId,
                backupPoint,
                startedAt,
                progress,
                ActivityLogAction.BACKUP_DOWNLOAD)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun onFetchBackupDownloadStatusActionCallRestClient() = test {
        val payload = FetchBackupDownloadStatePayload(siteModel)
        whenever(activityLogRestClient.fetchBackupDownloadState(siteModel)).thenReturn(
                FetchedBackupDownloadStatePayload(
                        null,
                        siteModel
                )
        )
        val action = ActivityLogActionBuilder.newFetchBackupDownloadStateAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchBackupDownloadState(siteModel)
    }

    @Test
    fun storeFetchedBackupDownloadStatusToDb() = test {
        val backupDownloadStatusModel = mock<BackupDownloadStatusModel>()
        val payload = FetchedBackupDownloadStatePayload(backupDownloadStatusModel, siteModel)
        whenever(activityLogRestClient.fetchBackupDownloadState(siteModel)).thenReturn(payload)

        val fetchAction =
                ActivityLogActionBuilder.newFetchBackupDownloadStateAction(FetchBackupDownloadStatePayload(siteModel))
        activityLogStore.onAction(fetchAction)

        verify(activityLogSqlUtils).replaceBackupDownloadStatus(siteModel, backupDownloadStatusModel)
        val expectedChangeEvent =
                ActivityLogStore.OnBackupDownloadStatusFetched(ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnBackupDownloadStatusFromDb() {
        val backupDownloadStatusModel = mock<BackupDownloadStatusModel>()
        whenever(activityLogSqlUtils.getBackupDownloadStatusForSite(siteModel))
                .thenReturn(backupDownloadStatusModel)

        val backDownloadStatusFromDb = activityLogStore.getBackupDownloadStatusForSite(siteModel)

        verify(activityLogSqlUtils).getBackupDownloadStatusForSite(siteModel)
        assertEquals(backupDownloadStatusModel, backDownloadStatusFromDb)
    }

    @Test
    fun storeFetchedEmptyRewindStatusRemoveFromDb() = test {
        whenever(activityLogRestClient.fetchActivityRewind(siteModel))
                .thenReturn(FetchedRewindStatePayload(null, siteModel))

        val fetchAction = ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(siteModel))
        activityLogStore.onAction(fetchAction)

        verify(activityLogSqlUtils).deleteRewindStatus(siteModel)
    }

    @Test
    fun storeFetchedEmptyBackupDownloadStatusRemoveFromDb() = test {
        whenever(activityLogRestClient.fetchBackupDownloadState(siteModel))
                .thenReturn(FetchedBackupDownloadStatePayload(null, siteModel))

        val fetchAction =
                ActivityLogActionBuilder.newFetchBackupDownloadStateAction(FetchBackupDownloadStatePayload(siteModel))
        activityLogStore.onAction(fetchAction)

        verify(activityLogSqlUtils).deleteBackupDownloadStatus(siteModel)
    }

    @Test
    fun onDismissBackupDownloadActionCallRestClient() = test {
        whenever(activityLogRestClient.dismissBackupDownload(eq(siteModel), any())).thenReturn(
                DismissBackupDownloadResultPayload(
                        100L,
                        10L,
                        true
                )
        )

        val downloadId = 10L
        val payload = DismissBackupDownloadPayload(siteModel, downloadId)
        val action = ActivityLogActionBuilder.newDismissBackupDownloadAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).dismissBackupDownload(siteModel, downloadId)
    }

    @Test
    fun emitsDismissBackupDownloadResult() = test {
        val downloadId = 10L
        val isDismissed = true

        val payload = DismissBackupDownloadResultPayload(
                siteModel.siteId,
                downloadId,
                isDismissed)
        whenever(activityLogRestClient.dismissBackupDownload(siteModel, downloadId)).thenReturn(payload)

        activityLogStore.onAction(ActivityLogActionBuilder.newDismissBackupDownloadAction(DismissBackupDownloadPayload(
                siteModel,
                downloadId)))

        val expectedChangeEvent = ActivityLogStore.OnDismissBackupDownload(
                downloadId,
                isDismissed,
                ActivityLogAction.DISMISS_BACKUP_DOWNLOAD)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    private suspend fun initRestClient(
        activityModels: List<ActivityLogModel>,
        rowsAffected: Int,
        offset: Int = OFFSET,
        number: Int = PAGE_SIZE,
        totalItems: Int = PAGE_SIZE
    ): Action<*> {
        val requestPayload = FetchActivityLogPayload(siteModel)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(requestPayload)

        val payload = FetchedActivityLogPayload(activityModels, siteModel, totalItems, number, offset)
        whenever(activityLogRestClient.fetchActivity(requestPayload, number, offset)).thenReturn(payload)
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)
        return action
    }

    companion object {
        private const val OFFSET = 0
        private const val PAGE_SIZE = 100
    }
}
