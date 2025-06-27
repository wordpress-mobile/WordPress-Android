package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.GetSubscriberParams
import uniffi.wp_api.IndividualSubscriberStats
import uniffi.wp_api.IndividualSubscriberStatsParams
import uniffi.wp_api.Subscriber
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscriberDetailViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    @Inject
    lateinit var accountStore: AccountStore

    private lateinit var wpComApiClient: WpComApiClient

    init {
        launch {
            wpComApiClient = WpComApiClient(
                WpAuthenticationProvider.staticWithAuth(
                    WpAuthentication.Bearer(token = accountStore.accessToken!!)
                )
            )
        }
    }

    suspend fun fetchSubscriberWithStats(
        siteId: ULong,
        userId: Long
    ): SubscriberWithStats? {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.subscribers().getSubscriber(
                wpComSiteId = siteId,
                params = GetSubscriberParams.WpCom(userId)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                val subscriber = response.response.data
                appLogWrapper.d(AppLog.T.MAIN, "Fetched subscriber: $subscriber")
                val stats = fetchSubscriberStats(siteId, subscriber.subscriptionId)
                return SubscriberWithStats(
                    subscriber = subscriber,
                    stats = stats
                )
            }

            else -> {
                appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $response")
                return null
            }
        }
    }

    private suspend fun fetchSubscriberStats(
        siteId: ULong,
        subscriptionId: ULong
    ): IndividualSubscriberStats? {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.subscribers().individualSubscriberStats(
                wpComSiteId = siteId,
                params = IndividualSubscriberStatsParams(
                    subscriptionId = subscriptionId
                )
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                val stats = response.response.data
                appLogWrapper.d(AppLog.T.MAIN, "Fetched subscriber stats: $stats")
                return stats
            }

            else -> {
                appLogWrapper.e(AppLog.T.MAIN, "Fetch subscribers failed: $response")
                return null
            }
        }
    }

    data class SubscriberWithStats(
        val subscriber: Subscriber,
        val stats: IndividualSubscriberStats?
    )
}
