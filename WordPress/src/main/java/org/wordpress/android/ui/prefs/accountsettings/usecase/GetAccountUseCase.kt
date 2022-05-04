package org.wordpress.android.ui.prefs.accountsettings.usecase

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class GetAccountUseCase @Inject constructor(
    private val accountStore: AccountStore
) : GetAccountInteractor {
    override val account : AccountModel = accountStore.account
}

interface GetAccountInteractor{
    val account : AccountModel
}
