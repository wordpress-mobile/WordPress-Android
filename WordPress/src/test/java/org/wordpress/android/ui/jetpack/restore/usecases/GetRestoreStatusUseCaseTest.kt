package org.wordpress.android.ui.jetpack.restore.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

private const val REWIND_ID = "rewindId"
private const val RESTORE_ID = 123456789L
private const val PROGRESS = 50
private const val MESSAGE = "message"
private const val CURRENT_ENTRY = "current entry"

private val PUBLISHED = Date()

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetRestoreStatusUseCaseTest {
    private lateinit var useCase: GetRestoreStatusUseCase

    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var site: SiteModel

    @Before
    fun setup() = test {
        useCase = GetRestoreStatusUseCase(networkUtilsWrapper, activityLogStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))
        whenever(activityLogStore.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activityLogModel())
    }

    @Test
    fun `given no network without restore id, when restore status triggers, then return network unavailable`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.getRestoreStatus(site, null).toList()

        assertThat(result).contains(Failure.NetworkUnavailable)
    }

    @Test
    fun `given no network with restore id, when restore status triggers, then return network unavailable`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

        assertThat(result).contains(Failure.NetworkUnavailable)
    }

    @Test
    fun `given failure without restore id, when restore status triggers, then return remote request failure`() =
            test {
                whenever(activityLogStore.fetchActivitiesRewind(any()))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given failure with restore id, when restore status triggers, then return failure`() =
            test {
                whenever(activityLogStore.fetchActivitiesRewind(any()))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given finished without restore id and rewind id, when restore status triggers, then return complete`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(Complete(REWIND_ID, RESTORE_ID, PUBLISHED))
    }

    @Test
    fun `given finished with restore id and rewind id, when restore status triggers, then return complete`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(Complete(REWIND_ID, RESTORE_ID, PUBLISHED))
    }

    @Test
    fun `given finished without restore id no rewind id, when restore status triggers, then return failure`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(null, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given finished with restore id no rewind id, when restore status triggers, then return failure`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(null, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given failed without restore id, when restore status triggers, then return failure`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FAILED))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given failed with restore id, when restore status triggers, then return failure`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FAILED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given running without restore id, when restore status triggers, then return progress`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.RUNNING))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }

    @Test
    fun `given running with restore id, when restore status triggers, then return progress`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.RUNNING))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }

    @Test
    fun `given queued without restore id, when restore status triggers, then return progress`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.QUEUED))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }

    @Test
    fun `given queued with restore id, when restore status triggers, then return progress`() =
            test {
                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.QUEUED))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }

    @Test
    fun `given get status model is null without restoreId, when restore status triggers, then return empty`() = test {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(null)

        val result = useCase.getRestoreStatus(site, null).toList()

        assertThat(result).contains(RestoreRequestState.Empty)
    }

    @Test
    fun `given get status model is null with restoreId, when restore status triggers, then return empty`() = test {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(null)

        val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

        assertThat(result).contains(RestoreRequestState.Empty)
    }

    @Test
    fun `given max fetch retries exceeded, when restore status triggers, then return remote request failure`() =
            test {
                whenever(activityLogStore.fetchActivitiesRewind(any()))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))

                val result = useCase.getRestoreStatus(site, null).toList()

                assertThat(result).size().isEqualTo(1)
                assertThat(result).isEqualTo(listOf(RemoteRequestFailure))
            }

    @Test
    fun `given fetch error under retry count, when restore status triggers, then return progress`() =
            test {
                whenever(activityLogStore.fetchActivitiesRewind(any()))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))
                        .thenReturn(OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR), FETCH_REWIND_STATE))
                        .thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))

                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.RUNNING))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }

    @Test
    fun `given no network available under retry count, when restore status triggers, then return progress`() =
            test {
                whenever(networkUtilsWrapper.isNetworkAvailable())
                        .thenReturn(false)
                        .thenReturn(false)
                        .thenReturn(true)
                whenever(activityLogStore.fetchActivitiesRewind(any()))
                        .thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))

                whenever(activityLogStore.getRewindStatusForSite(site))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.RUNNING))
                        .thenReturn(rewindStatusModel(REWIND_ID, Rewind.Status.FINISHED))

                val result = useCase.getRestoreStatus(site, RESTORE_ID).toList()

                assertThat(result).contains(
                        Progress(REWIND_ID, PROGRESS, MESSAGE, CURRENT_ENTRY, PUBLISHED),
                        Complete(REWIND_ID, RESTORE_ID, PUBLISHED)
                )
            }
    /* PRIVATE */

    private fun activityLogModel() = ActivityLogModel(
            activityID = "activityID",
            summary = "summary",
            content = null,
            name = null,
            type = null,
            gridicon = null,
            status = null,
            rewindable = null,
            rewindID = null,
            published = PUBLISHED,
            actor = null
    )

    private fun rewindStatusModel(
        rewindId: String?,
        status: Rewind.Status
    ) = RewindStatusModel(
            state = State.ACTIVE,
            reason = null,
            lastUpdated = PUBLISHED,
            canAutoconfigure = null,
            credentials = null,
            rewind = rewind(rewindId, status)
    )

    private fun rewind(
        rewindId: String?,
        status: Rewind.Status
    ) = Rewind(
            rewindId = rewindId,
            restoreId = RESTORE_ID,
            status = status,
            progress = PROGRESS,
            reason = null,
            message = MESSAGE,
            currentEntry = CURRENT_ENTRY
    )
}
