package org.wordpress.android.ui.prefs.accountsettings.usecase

import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult
import org.wordpress.android.fluxc.network.rest.wpcom.account.closeAccount
import javax.inject.Inject

class AccountClosureUseCase @Inject constructor(
    private val accountRestClient: AccountRestClient,
) {
    fun closeAccount(onResult: (CloseAccountResult) -> Unit) = accountRestClient.closeAccount(onResult)
}
