package org.wordpress.android.ui.prefs.accountsettings

import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsInteractor
import javax.inject.Inject

class AccountSettingsRepository @Inject constructor(
    fetchNewAccountSettingsUseCase: FetchAccountSettingsInteractor,
    pushAccountSettingsUseCase: PushAccountSettingsInteractor,
    getSitesUseCase: GetSitesInteractor,
    getAccountUseCase: GetAccountInteractor
) : FetchAccountSettingsInteractor by fetchNewAccountSettingsUseCase,
        PushAccountSettingsInteractor by pushAccountSettingsUseCase,
        GetSitesInteractor by getSitesUseCase,
        GetAccountInteractor by getAccountUseCase
