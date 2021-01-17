package org.wordpress.android.ui.jetpack.scan.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ScanAction.FETCH_FIX_THREATS_STATUS
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel.FixStatus
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusErrorType
import org.wordpress.android.fluxc.store.ScanStore.OnFixThreatsStatusFetched
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.Complete
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.InProgress
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
class FetchFixThreatsStatusUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: FetchFixThreatsStatusUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var scanStore: ScanStore

    private val delayInMs = 0L
    private val fakeSiteId = 1L
    private val fakeThreatId = 11L
    private val fakeFixThreatsStatusModel = FixThreatStatusModel(fakeThreatId, FixStatus.IN_PROGRESS)

    private val storeResultWithInProgressFixStatusModel = OnFixThreatsStatusFetched(
        fakeSiteId,
        listOf(fakeFixThreatsStatusModel),
        FETCH_FIX_THREATS_STATUS
    )

    private val storeResultWithFixedFixStatusModel = storeResultWithInProgressFixStatusModel.copy(
        fixThreatStatusModels = listOf(fakeFixThreatsStatusModel.copy(status = FixStatus.FIXED))
    )

    @Before
    fun setup() {
        useCase = FetchFixThreatsStatusUseCase(networkUtilsWrapper, scanStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when threats fix status is fetched, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
            .toList(mutableListOf())
            .last()

        assertThat(useCaseResult).isEqualTo(Failure.NetworkUnavailable)
    }

    @Test
    fun `when in progress threats fix status is fetched, then polling occurs until in progress status changes`() =
        test {
            whenever(scanStore.fetchFixThreatsStatus(any()))
                .thenReturn(storeResultWithInProgressFixStatusModel)
                .thenReturn(storeResultWithInProgressFixStatusModel)
                .thenReturn(storeResultWithFixedFixStatusModel)

            val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId), delayInMs)
                .toList(mutableListOf())

            verify(scanStore, times(3)).fetchFixThreatsStatus(any())
            assertThat(useCaseResult).containsSequence(InProgress, InProgress, Complete)
        }

    @Test
    fun `given threats fixed successfully, when threats fix status is fetched, then Complete is returned`() = test {
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithFixedFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
            .toList(mutableListOf())
            .last()

        assertThat(useCaseResult).isEqualTo(Complete)
    }

    @Test
    fun `given invalid response, when threats fix status is fetched, then RemoteRequestFailure is returned`() = test {
        val storeResultWithGenericError = OnFixThreatsStatusFetched(
            fakeSiteId,
            FixThreatsStatusError(FixThreatsStatusErrorType.INVALID_RESPONSE),
            FETCH_FIX_THREATS_STATUS
        )
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithGenericError)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
            .toList(mutableListOf())
            .last()

        assertThat(useCaseResult).isEqualTo(Failure.RemoteRequestFailure)
    }

    @Test
    fun `when threats fix status is fetched, then FixFailure is returned if result model contains error`() = test {
        val storeResultWithErrorFixStatusModel = OnFixThreatsStatusFetched(
            fakeSiteId,
            listOf(fakeFixThreatsStatusModel.copy(status = FixStatus.UNKNOWN, error = "not_found")),
            FETCH_FIX_THREATS_STATUS
        )
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithErrorFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
            .toList(mutableListOf())
            .last()

        assertThat(useCaseResult).isEqualTo(Failure.FixFailure)
    }
}
