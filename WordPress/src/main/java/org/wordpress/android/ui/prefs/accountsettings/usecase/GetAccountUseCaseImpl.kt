package org.wordpress.android.ui.prefs.accountsettings.usecase

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class GetAccountUseCaseImpl @Inject constructor(
    private val accountStore: AccountStore
) : GetAccountUseCase {
    override val account: AccountModel
        get() = accountStore.account
}

interface GetAccountUseCase {
    val account: AccountModel
}
