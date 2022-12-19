package org.wordpress.android.ui.prefs.accountsettings.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.accountsettings.module.DEFAULT_CONTINUATION
import org.wordpress.android.ui.utils.ContinuationWrapper
import javax.inject.Inject
import javax.inject.Named

class FetchAccountSettingsUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Named(DEFAULT_CONTINUATION) private val continuationWrapper: ContinuationWrapper<OnAccountChanged>,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchNewSettings(): OnAccountChanged = withContext(ioDispatcher) {
        dispatcher.register(this@FetchAccountSettingsUseCase)
        continuationWrapper.suspendCoroutine {
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    fun onAccountChanged(event: OnAccountChanged) {
        continuationWrapper.continueWith(event)
        dispatcher.unregister(this)
    }
}
