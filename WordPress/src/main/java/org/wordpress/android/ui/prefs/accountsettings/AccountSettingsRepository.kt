package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
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

    private var fetchNewSettingscontinuation: Continuation<OnAccountChanged>? = null
    private var continuationList = mutableListOf<Continuation<OnAccountChanged>>()

    val account: AccountModel
        get() = accountStore.account

    suspend fun getSitesAccessedViaWPComRest(): List<SiteModel> = withContext(ioDispatcher) {
        siteStore.sitesAccessedViaWPComRest }

    suspend fun getSite(siteRemoteId: Long): SiteModel? = withContext(ioDispatcher) {
        siteStore.getSiteBySiteId(siteRemoteId)
    }

    suspend fun fetchNewSettings(): OnAccountChanged = withContext(ioDispatcher) {
        suspendCancellableCoroutine {
            fetchNewSettingscontinuation = it
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
        }
    }

    suspend fun updatePrimaryBlog(blogId: String): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["primary_site_ID"] = blogId }
        return updateAccountSettings(addPayload)
    }

    suspend fun cancelPendingEmailChange(): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["user_email_change_pending"] = "false" }
        return updateAccountSettings(addPayload)
    }

    suspend fun updateEmail(newEmail: String): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["user_email"] = newEmail }
        return updateAccountSettings(addPayload)
    }

    suspend fun updateWebAddress(newWebAddress: String?): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["user_URL"] = newWebAddress }
        return updateAccountSettings(addPayload)
    }

    suspend fun updatePassword(newPassword: String?): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["password"] = newPassword }
        return updateAccountSettings(addPayload)
    }

    private suspend fun updateAccountSettings(addPayload: (PushAccountSettingsPayload) -> Unit): OnAccountChanged =
            withContext(ioDispatcher) {
                suspendCancellableCoroutine {
                    continuationList.add(it)
                    val payload = PushAccountSettingsPayload()
                    payload.params = HashMap()
                    addPayload(payload)
                    dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
                }
            }

    @Subscribe
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            fetchNewSettingscontinuation?.resume(event)
            fetchNewSettingscontinuation = null
        } else if (event.causeOfChange == AccountAction.PUSHED_SETTINGS) {
            continuationList.get(0)?.resume(event)
            continuationList.removeAt(0)
        }
    }

    fun onCleanUp() {
        dispatcher.unregister(this)
    }
}
