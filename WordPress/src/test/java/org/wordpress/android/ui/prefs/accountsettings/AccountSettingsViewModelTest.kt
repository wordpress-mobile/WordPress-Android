package org.wordpress.android.ui.prefs.accountsettings

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.SiteViewModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
class AccountSettingsViewModelTest : BaseUnitTest(){

    private lateinit var viewModel: AccountSettingsViewModel
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var accountsSettingsRepository:AccountSettingsRepository
    @Mock private lateinit var account: AccountModel

    private val siteViewModels = mutableListOf<SiteViewModel>().apply {
        add(SiteViewModel("HappyDay", 1L, "http://happyday.wordpress.com"))
        add(SiteViewModel("WonderLand", 2L, "http://wonderland.wordpress.com"))
        add(SiteViewModel("FantasyBooks", 3L, "http://fantasybooks.wordpress.com"))
    }

    @Before
    fun setUp() = test {

        whenever(account.primarySiteId).thenReturn(3L)
        whenever(account.userName).thenReturn("old_wordpressuser_username")
        whenever(account.displayName).thenReturn("old_wordpressuser_displayname")
        whenever(account.email).thenReturn("old_wordpressuser@gmail.com")
        whenever(account.newEmail).thenReturn("")
        whenever(account.webAddress).thenReturn("http://old_wordpressuser.com")
        whenever(account.pendingEmailChange).thenReturn(false)
        whenever(account.usernameCanBeChanged).thenReturn(false)
        whenever(accountsSettingsRepository.account).thenReturn(account)

        val sites = siteViewModels.map {
            SiteModel().apply {
                this.siteId = it.siteId
                this.name = it.siteName
                this.url = it.homeURLOrHostName
            }
        }
        whenever(accountsSettingsRepository.getSitesAccessedViaWPComRest()).thenReturn(sites)
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                accountsSettingsRepository
        )
    }
}
