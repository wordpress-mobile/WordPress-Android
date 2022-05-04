package org.wordpress.android.ui.prefs.accountsettings.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.utils.ContinuationWrapperWithConcurrency
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject
import javax.inject.Named

class PushAccountSettingsUseCase @Inject constructor(
    private val eventBusWrapper: EventBusWrapper,
    private val continuationWrapperWithConcurrency: ContinuationWrapperWithConcurrency<OnAccountChanged>,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
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

    suspend fun updateWebAddress(newWebAddress: String): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["user_URL"] = newWebAddress }
        return updateAccountSettings(addPayload)
    }

    suspend fun updatePassword(newPassword: String): OnAccountChanged {
        val addPayload: (PushAccountSettingsPayload) -> Unit = { it.params["password"] = newPassword }
        return updateAccountSettings(addPayload)
    }

    private suspend fun updateAccountSettings(addPayload: (PushAccountSettingsPayload) -> Unit): OnAccountChanged =
            withContext(ioDispatcher) {
                continuationWrapperWithConcurrency.suspendCoroutine {
                    val payload = PushAccountSettingsPayload()
                    payload.params = HashMap()
                    addPayload(payload)
                    eventBusWrapper.registerIfNotAlready(this)
                    eventBusWrapper.post(AccountActionBuilder.newPushSettingsAction(payload))
                }
            }

    @Subscribe(threadMode = BACKGROUND)
    fun onAccountChanged(event: OnAccountChanged) {
        continuationWrapperWithConcurrency.continueWith( event)
        eventBusWrapper.unregister(this)
    }
}
