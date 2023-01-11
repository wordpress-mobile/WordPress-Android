package org.wordpress.android.ui.jetpack.restore.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.action.ActivityLogAction.REWIND
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Reason
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.QUEUED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewind
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType.API_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class PostRestoreUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: PostRestoreUseCase

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var activityLogStore: ActivityLogStore

    @Mock
    lateinit var siteModel: SiteModel

    private val rewindId = "rewindId"
    private val restoreId = 1L
    private val types: RewindRequestTypes = RewindRequestTypes(
        themes = true,
        plugins = true,
        uploads = true,
        sqls = true,
        roots = true,
        contents = true
    )

    @Before
    fun setup() = test {
        useCase = PostRestoreUseCase(
            networkUtilsWrapper,
            activityLogStore,
            testDispatcher()
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(
            OnRewindStatusFetched(ActivityLogAction.FETCH_REWIND_STATE)
        )
    }

    @Test
    fun `given no network, when rewind is triggered, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `given invalid response, when restore is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, RewindError(INVALID_RESPONSE), REWIND))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given generic error response, when restore is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, RewindError(GENERIC_ERROR), REWIND))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given api error response, when restore is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, RewindError(API_ERROR), REWIND))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `when restore is triggered successfully, then Success is returned`() = test {
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, restoreId, REWIND))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(Success(requestRewindId = rewindId, rewindId = rewindId, restoreId = restoreId))
    }

    @Test
    fun `given fetch error, then RemoteRequestFailure is returned`() = test {
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(
            OnRewindStatusFetched(
                RewindStatusError(RewindStatusErrorType.GENERIC_ERROR),
                ActivityLogAction.FETCH_REWIND_STATE
            )
        )

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given fetch success, when process is running, then OtherRequestRunning is returned`() = test {
        whenever(activityLogStore.getRewindStatusForSite(siteModel))
            .thenReturn(buildStatusModel(RUNNING))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(OtherRequestRunning)
    }

    @Test
    fun `given fetch success, when process is queued, then OtherRequestRunning is returned`() = test {
        whenever(activityLogStore.getRewindStatusForSite(siteModel))
            .thenReturn(buildStatusModel(QUEUED))

        val result = useCase.postRestoreRequest(rewindId, siteModel, types)

        assertThat(result).isEqualTo(OtherRequestRunning)
    }

    private fun buildStatusModel(status: Status) = RewindStatusModel(
        state = ACTIVE,
        reason = Reason.NO_REASON,
        lastUpdated = Date(1609690147756),
        canAutoconfigure = null,
        credentials = null,
        rewind = Rewind(
            rewindId = rewindId,
            restoreId = restoreId,
            status = status,
            progress = null,
            reason = null,
            message = null,
            currentEntry = null
        )
    )
}
