package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.util.DisplayUtilsWrapper

class MySiteViewModelTest : BaseUnitTest() {
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    private lateinit var viewModel: MySiteViewModel
    private lateinit var site: SiteModel
    private val onSiteChange = MutableLiveData<Int>()
    private lateinit var uiModels: MutableList<UiModel>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val siteId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        onSiteChange.value = null
        whenever(selectedSiteRepository.siteSelected).thenReturn(onSiteChange)
        viewModel = MySiteViewModel(TEST_DISPATCHER, accountStore, selectedSiteRepository, displayUtilsWrapper)
        uiModels = mutableListOf()
        viewModel.uiModel.observeForever {
            if (it != null) {
                uiModels.add(it)
            }
        }
        site = SiteModel()
        site.id = siteId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
    }

    @Test
    fun `model is empty with no selected site`() {
        onSiteChange.postValue(null)

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
    }

    @Test
    fun `account avatar url initial value is empty`() {
        assertThat(uiModels).hasSize(1)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `account avatar url value is emitted after refresh`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refresh()

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().accountAvatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `account avatar url value is emitted after refresh even if new value is the same`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refresh()
        viewModel.refresh()

        assertThat(uiModels).hasSize(3)
    }

    @Test
    fun `account avatar url value is emitted after refresh even if new value is empty`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refresh()

        setupAccount(buildAccountWithAvatarUrl(null))

        viewModel.refresh()

        assertThat(uiModels).hasSize(3)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `account avatar url value is emitted after refresh even if account is null`() {
        setupAccount(null)

        viewModel.refresh()

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `when no site is selected and screen height is higher than 600 pixels, show empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(600)

        onSiteChange.postValue(null)

        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
        assertThat((uiModels.last().state as State.NoSites).shouldShowImage).isTrue
    }

    @Test
    fun `when no site is selected and screen height is lower than 600 pixels, hide empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(500)

        onSiteChange.postValue(null)

        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
        assertThat((uiModels.last().state as State.NoSites).shouldShowImage).isFalse
    }

    private fun setupAccount(account: AccountModel?) = whenever(accountStore.account).thenReturn(account)

    private fun buildAccountWithAvatarUrl(avatarUrl: String?) = AccountModel().apply { this.avatarUrl = avatarUrl }
}
