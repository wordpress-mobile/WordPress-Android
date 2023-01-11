package org.wordpress.android.ui.stats.refresh

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.action.JetpackAction.ACTIVATE_STATS_MODULE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleError
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType
import org.wordpress.android.fluxc.store.JetpackStore.OnActivateStatsModule
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.stats.refresh.StatsModuleActivateRequestState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class StatsModuleActivateUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: StatsModuleActivateUseCase

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var siteModel: SiteModel

    @Mock
    lateinit var jetpackStore: JetpackStore

    @Before
    fun setup() = test {
        useCase = StatsModuleActivateUseCase(
            networkUtilsWrapper,
            jetpackStore,
            testDispatcher()
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when activate is triggered, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `given invalid response, when activate is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(jetpackStore.activateStatsModule(any()))
            .thenReturn(
                OnActivateStatsModule(
                    ActivateStatsModuleError(ActivateStatsModuleErrorType.INVALID_RESPONSE),
                    ACTIVATE_STATS_MODULE
                )
            )

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given generic error response, when activate is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(jetpackStore.activateStatsModule(any()))
            .thenReturn(
                OnActivateStatsModule(
                    ActivateStatsModuleError(ActivateStatsModuleErrorType.GENERIC_ERROR),
                    ACTIVATE_STATS_MODULE
                )
            )

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given api error response, when activate is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(jetpackStore.activateStatsModule(any()))
            .thenReturn(
                OnActivateStatsModule(
                    ActivateStatsModuleError(ActivateStatsModuleErrorType.API_ERROR),
                    ACTIVATE_STATS_MODULE
                )
            )

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `given auth error response, when activate is triggered, then RemoteRequestFailure is returned`() = test {
        whenever(jetpackStore.activateStatsModule(any()))
            .thenReturn(
                OnActivateStatsModule(
                    ActivateStatsModuleError(ActivateStatsModuleErrorType.AUTHORIZATION_REQUIRED),
                    ACTIVATE_STATS_MODULE
                )
            )

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `when activate is triggered successfully, then Success is returned`() = test {
        whenever(jetpackStore.activateStatsModule(any())).thenReturn(OnActivateStatsModule(ACTIVATE_STATS_MODULE))

        val result = useCase.postActivateStatsModule(siteModel)

        Assertions.assertThat(result).isEqualTo(Success)
    }
}
