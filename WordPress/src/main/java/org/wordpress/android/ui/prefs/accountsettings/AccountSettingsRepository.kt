package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class AccountSettingsRepository @Inject constructor(
    private var dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private var accountStore: AccountStore,
    private var siteStore: SiteStore
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanUp() {
        dispatcher.unregister(this)
    }
}
