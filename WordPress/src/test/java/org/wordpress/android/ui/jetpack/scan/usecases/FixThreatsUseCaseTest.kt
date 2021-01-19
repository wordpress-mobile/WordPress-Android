package org.wordpress.android.ui.jetpack.scan.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ScanAction.FIX_THREATS
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.ScanStore.OnFixThreatsStarted
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
class FixThreatsUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: FixThreatsUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var scanStore: ScanStore

    private val fakeSiteId = 1L
    private val fakeThreatIds = listOf(11L)

    @Before
    fun setup() = test {
        useCase = FixThreatsUseCase(networkUtilsWrapper, scanStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when fix threats is triggered, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fixThreats(fakeSiteId, fakeThreatIds)

        assertThat(result).isEqualTo(Failure.NetworkUnavailable)
    }

    @Test
    fun `given invalid response, when fix threats is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(scanStore.fixThreats(any())).thenReturn(
            OnFixThreatsStarted(FixThreatsError(INVALID_RESPONSE), FIX_THREATS)
        )

        val result = useCase.fixThreats(fakeSiteId, fakeThreatIds)

        assertThat(result).isEqualTo(Failure.RemoteRequestFailure)
    }

    @Test
    fun `when fix threats is triggered successfully, then Success is returned`() = test {
        whenever(scanStore.fixThreats(any())).thenReturn(OnFixThreatsStarted(FIX_THREATS))

        val result = useCase.fixThreats(fakeSiteId, fakeThreatIds)

        assertThat(result).isEqualTo(Success)
    }
}
