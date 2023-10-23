package org.wordpress.android.ui.prefs

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.dispatchAndAwait
import javax.inject.Inject

class PrivacySettingsRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
) {
    companion object {
        private const val SETTING_TRACKS_OPT_OUT = "tracks_opt_out"
    }

    suspend fun updateTracksSetting(isEnabled: Boolean) = AccountActionBuilder.newPushSettingsAction(
        AccountStore.PushAccountSettingsPayload().apply {
            params = mapOf(SETTING_TRACKS_OPT_OUT to !isEnabled)
        }
    ).let { action ->
        dispatcher.dispatchAndAwait<AccountStore.PushAccountSettingsPayload?, AccountStore.OnAccountChanged>(
            action
        )
    }.let { event ->
        if (event.isError) {
            Result.failure(OnChangedException(event.error))
        } else {
            analyticsTrackerWrapper.hasUserOptedOut = accountStore.account.tracksOptOut
            Result.success(Unit)
        }
    }

    fun isUserWPCOM(): Boolean {
        return accountStore.hasAccessToken()
    }
}

class OnChangedException(val error: OnChangedError, override val message: String? = null) : Exception()
