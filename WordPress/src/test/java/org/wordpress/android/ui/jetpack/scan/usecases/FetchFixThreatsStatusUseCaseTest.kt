package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.MainCoroutineScopeRule
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
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.NotStarted
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class FetchFixThreatsStatusUseCaseTest : BaseUnitTest() {
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var useCase: FetchFixThreatsStatusUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var scanStore: ScanStore

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
    fun `when in progress threats fix status fetched, then polling occurs until in progress status changes`() = test {
        whenever(scanStore.fetchFixThreatsStatus(any()))
                .thenReturn(storeResultWithInProgressFixStatusModel)
                .thenReturn(storeResultWithInProgressFixStatusModel)
                .thenReturn(storeResultWithFixedFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
                .toList(mutableListOf())
        coroutineScope.advanceTimeBy(FETCH_FIX_THREATS_STATUS_DELAY_MILLIS)

        verify(scanStore, times(3)).fetchFixThreatsStatus(any())
        assertThat(useCaseResult).containsSequence(
                InProgress(listOf(fakeThreatId)),
                InProgress(listOf(fakeThreatId)),
                Complete(fixedThreatsCount = 1)
        )
    }

    @Test
    fun `given threats fixed successfully, when threats fix status is fetched, then Complete is returned`() = test {
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithFixedFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
                .toList(mutableListOf())
                .last()

        assertThat(useCaseResult).isEqualTo(Complete(fixedThreatsCount = 1))
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
    fun `given result models contain error, when threats fix status is fetched, then FixFailure is returned`() = test {
        val fixThreatsStatusModels = listOf(
                fakeFixThreatsStatusModel.copy(status = FixStatus.FIXED),
                fakeFixThreatsStatusModel.copy(status = FixStatus.UNKNOWN, error = "not_found")
        )
        val storeResultWithErrorFixStatusModel = OnFixThreatsStatusFetched(
                fakeSiteId,
                fixThreatsStatusModels,
                FETCH_FIX_THREATS_STATUS
        )
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithErrorFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(1L, 2L))
                .toList(mutableListOf())
                .last()

        assertThat(useCaseResult).isEqualTo(
                Failure.FixFailure(
                        containsOnlyErrors = false,
                        mightBeMissingCredentials = false
                )
        )
    }

    @Test
    fun `given result models contains only errors, then FixFailure - containsOnlyErrors is true`() = test {
        val fixThreatsStatusModels = listOf(
                fakeFixThreatsStatusModel.copy(status = FixStatus.NOT_FIXED),
                fakeFixThreatsStatusModel.copy(status = FixStatus.UNKNOWN, error = "not_found")
        )
        val storeResultWithErrorFixStatusModel = OnFixThreatsStatusFetched(
                fakeSiteId,
                fixThreatsStatusModels,
                FETCH_FIX_THREATS_STATUS
        )
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithErrorFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(1L, 2L))
                .toList(mutableListOf())
                .last()

        assertThat(useCaseResult).isEqualTo(
                Failure.FixFailure(
                        containsOnlyErrors = true,
                        mightBeMissingCredentials = false
                )
        )
    }

    @Test
    fun `given model contains not started state, when threats fix status is fetched, then NotStarted is returned`() =
            test {
                val storeResultWithErrorFixStatusModel = OnFixThreatsStatusFetched(
                        fakeSiteId,
                        listOf(fakeFixThreatsStatusModel.copy(status = FixStatus.NOT_STARTED)),
                        FETCH_FIX_THREATS_STATUS
                )
                whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithErrorFixStatusModel)

                val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(fakeThreatId))
                        .toList(mutableListOf())
                        .last()

                assertThat(useCaseResult).isEqualTo(NotStarted)
            }

    @Test
    fun `given all threats NOT_FIXED, when status is fetched, then FixFailure(containsOnly = true) returned`() = test {
        val fixThreatsStatusModels = listOf(
                fakeFixThreatsStatusModel.copy(status = FixStatus.NOT_FIXED),
                fakeFixThreatsStatusModel.copy(status = FixStatus.NOT_FIXED)
        )
        val storeResultWithErrorFixStatusModel = OnFixThreatsStatusFetched(
                fakeSiteId,
                fixThreatsStatusModels,
                FETCH_FIX_THREATS_STATUS
        )
        whenever(scanStore.fetchFixThreatsStatus(any())).thenReturn(storeResultWithErrorFixStatusModel)

        val useCaseResult = useCase.fetchFixThreatsStatus(fakeSiteId, listOf(1L, 2L))
                .toList(mutableListOf())
                .last()

        assertThat(useCaseResult).isEqualTo(
                Failure.FixFailure(
                        containsOnlyErrors = true,
                        mightBeMissingCredentials = true
                )
        )
    }
}
