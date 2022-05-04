package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsInteractor
import javax.inject.Inject
import javax.inject.Named

class AccountSettingsRepository @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val fetchNewAccountSettingsUseCase: FetchAccountSettingsInteractor,
    private val pushAccountSettingsUseCase: PushAccountSettingsInteractor
) : FetchAccountSettingsInteractor by fetchNewAccountSettingsUseCase,
        PushAccountSettingsInteractor by pushAccountSettingsUseCase {
    val account: AccountModel
        get() = accountStore.account

    suspend fun getSitesAccessedViaWPComRest(): List<SiteModel> = withContext(ioDispatcher) {
        siteStore.sitesAccessedViaWPComRest
    }

    suspend fun getSite(siteRemoteId: Long): SiteModel? = withContext(ioDispatcher) {
        siteStore.getSiteBySiteId(siteRemoteId)
    }
}
