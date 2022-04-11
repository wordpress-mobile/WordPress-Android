package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.InternalCoroutinesApi
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
class AccountSettingsViewModelTest : BaseUnitTest(){

    private lateinit var viewModel: AccountSettingsViewModel
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var accountsSettingsRepository:AccountSettingsRepository
    @Mock private lateinit var account: AccountModel

}
