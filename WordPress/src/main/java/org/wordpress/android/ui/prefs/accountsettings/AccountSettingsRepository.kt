package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import javax.inject.Inject
import javax.inject.Named

class AccountSettingsRepository @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val fetchNewAccountSettingsUseCase: FetchAccountSettingsUseCase,
    private val pushAccountSettingsUseCase: PushAccountSettingsUseCase
) {

    val account: AccountModel
        get() = accountStore.account

    suspend fun getSitesAccessedViaWPComRest(): List<SiteModel> = withContext(ioDispatcher) {
        siteStore.sitesAccessedViaWPComRest
    }

    suspend fun getSite(siteRemoteId: Long): SiteModel? = withContext(ioDispatcher) {
        siteStore.getSiteBySiteId(siteRemoteId)
    }

    suspend fun fetchNewSettings(): OnAccountChanged = fetchNewAccountSettingsUseCase.fetchNewSettings()

    suspend fun updatePrimaryBlog(blogId: String) = pushAccountSettingsUseCase.updatePrimaryBlog(blogId)

    suspend fun cancelPendingEmailChange() = pushAccountSettingsUseCase.cancelPendingEmailChange()

    suspend fun updateEmail(newEmail: String) = pushAccountSettingsUseCase.updateEmail(newEmail)

    suspend fun updateWebAddress(newWebAddress: String) = pushAccountSettingsUseCase.updateWebAddress(newWebAddress)

    suspend fun updatePassword(newPassword: String) = pushAccountSettingsUseCase.updateWebAddress(newPassword)

}
