package org.wordpress.android.ui.prefs.accountsettings.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.utils.ContinuationWrapper
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject
import javax.inject.Named

class FetchAccountSettingsUseCase @Inject constructor(
    private val eventBusWrapper: EventBusWrapper,
    private val continuationWrapper: ContinuationWrapper<OnAccountChanged>,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : FetchAccountSettingsInteractor{
    override suspend fun fetchNewSettings(): OnAccountChanged = withContext(ioDispatcher) {
        continuationWrapper.suspendCoroutine {
            eventBusWrapper.registerIfNotAlready(this)
            eventBusWrapper.post(AccountActionBuilder.newFetchSettingsAction())
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    fun onAccountChanged(event: OnAccountChanged) {
        continuationWrapper.continueWith(event)
        eventBusWrapper.unregister(this)
    }
}

interface FetchAccountSettingsInteractor{
    suspend fun fetchNewSettings(): OnAccountChanged
}
