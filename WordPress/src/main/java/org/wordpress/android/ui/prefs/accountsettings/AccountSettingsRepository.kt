package org.wordpress.android.ui.prefs.accountsettings

import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import javax.inject.Inject

class AccountSettingsRepository @Inject constructor(
    fetchNewAccountSettingsUseCaseImpl: FetchAccountSettingsUseCase,
    pushAccountSettingsUseCaseImpl: PushAccountSettingsUseCase,
    getSitesUseCaseImpl: GetSitesUseCase,
    getAccountUseCaseImpl: GetAccountUseCase
) : FetchAccountSettingsUseCase by fetchNewAccountSettingsUseCaseImpl,
        PushAccountSettingsUseCase by pushAccountSettingsUseCaseImpl,
        GetSitesUseCase by getSitesUseCaseImpl,
        GetAccountUseCase by getAccountUseCaseImpl
