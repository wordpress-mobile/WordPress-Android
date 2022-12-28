package org.wordpress.android.ui.jetpack.scan.details.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.action.ScanAction.IGNORE_THREAT
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatError
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.ScanStore.OnIgnoreThreatStarted
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase.IgnoreThreatState.Failure
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase.IgnoreThreatState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class IgnoreThreatUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: IgnoreThreatUseCase
    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock
    lateinit var scanStore: ScanStore

    private val fakeSiteId = 1L
    private val fakeThreatId = 11L

    @Before
    fun setup() = test {
        useCase = IgnoreThreatUseCase(
            networkUtilsWrapper,
            scanStore,
            testDispatcher()
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when ignore threat is triggered, then the call fails due to network unavailability`() =
        test {
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

            val result = useCase.ignoreThreat(fakeSiteId, fakeThreatId)

            assertThat(result).isEqualTo(Failure.NetworkUnavailable)
        }

    @Test
    fun `given invalid response, when ignore threat is triggered, then the call fails`() = test {
        whenever(scanStore.ignoreThreat(any())).thenReturn(
            OnIgnoreThreatStarted(IgnoreThreatError(INVALID_RESPONSE), IGNORE_THREAT)
        )

        val result = useCase.ignoreThreat(fakeSiteId, fakeThreatId)

        assertThat(result).isEqualTo(Failure.RemoteRequestFailure)
    }

    @Test
    fun `given valid response, when ignore threat is triggered, then the call succeeds`() = test {
        whenever(scanStore.ignoreThreat(any())).thenReturn(OnIgnoreThreatStarted(IGNORE_THREAT))

        val result = useCase.ignoreThreat(fakeSiteId, fakeThreatId)

        assertThat(result).isEqualTo(Success)
    }
}
