package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class AccountSettingsRepository @Inject constructor(
    private var dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private var accountStore: AccountStore,
    private var siteStore: SiteStore
) {
    init {
        dispatcher.register(this)
    }

    private var continuation: Continuation<OnAccountChanged>? = null

    val account: AccountModel
        get() = accountStore.account

    fun getSitesAccessedViaWPComRest(): List<SiteModel> = siteStore.sitesAccessedViaWPComRest

    fun getSite(siteRemoteId: Long): SiteModel? {
        return siteStore.getSiteBySiteId(siteRemoteId)
    }

    private suspend fun updateAccountSettings(addPayload: (PushAccountSettingsPayload) -> Unit): OnAccountChanged = withContext(ioDispatcher) {
        suspendCancellableCoroutine {
            continuation = it
            val payload = PushAccountSettingsPayload()
            payload.params = HashMap()
            addPayload(payload)
            dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
        }
    }

    @Subscribe
    fun onAccountChanged(event: OnAccountChanged) {
        continuation?.resume(event)
        continuation = null
    }

    fun onCleanUp() {
        dispatcher.unregister(this)
    }
}
