package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenConnectJetpackForStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMediaScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenPagesScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenPostsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSite
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.StartLoginForJetpackStats
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.ICON_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.SWITCH_SITE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.TITLE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.URL_CLICK
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider

class MySiteViewModelTest : BaseUnitTest() {
    @Mock lateinit var siteInfoBlockBuilder: SiteInfoBlockBuilder
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<UiModel>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<NavigationAction>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private lateinit var site: SiteModel
    private lateinit var siteInfoBlock: SiteInfoBlock
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
        viewModel = MySiteViewModel(
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
                siteInfoBlockBuilder,
                accountStore,
                selectedSiteRepository,
                wpMediaUtilsWrapper,
                mediaUtilsWrapper,
                fluxCUtilsWrapper,
                contextProvider
        )
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        viewModel.uiModel.observeForever {
            uiModels.add(it)
        }
        viewModel.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }
        viewModel.onTextInputDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                textInputDialogModels.add(it)
            }
        }
        viewModel.onBasicDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                dialogModels.add(it)
            }
        }
        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        site = SiteModel()
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        siteInfoBlock = SiteInfoBlock(
                siteName,
                siteUrl,
                IconState.Visible(siteIcon),
                null,
                mock(),
                mock(),
                mock()
        )
        whenever(siteInfoBlockBuilder.buildSiteInfoBlock(eq(site), any(), any(), any(), any(), any())).thenReturn(
                siteInfoBlock
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `model is empty with no selected site`() {
        onSiteChange.postValue(null)

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().items).isEmpty()
    }

    @Test
    fun `model is contains header of selected site`() {
        onSiteChange.postValue(site)

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().items).hasSize(2)
        assertThat(uiModels.last().items.first() is SiteInfoBlock).isTrue()
    }

    @Test
    fun `site block title click shows snackbar message when network not available`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site block title click shows snackbar message when hasCapabilityManageOptions is false`() {
        site.hasCapabilityManageOptions = false
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(
                        UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint)
                )
        )
    }

    @Test
    fun `site block title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site block title click shows input dialog when editing allowed`() {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(snackbars).isEmpty()
        assertThat(textInputDialogModels.last()).isEqualTo(
                TextInputDialogModel(
                        callbackId = MySiteViewModel.SITE_NAME_CHANGE_CALLBACK_ID,
                        title = R.string.my_site_title_changer_dialog_title,
                        initialText = siteName,
                        hint = R.string.my_site_title_changer_dialog_hint,
                        isMultiline = false,
                        isInputEnabled = true
                )
        )
    }

    @Test
    fun `site block icon click shows change icon dialog when site has icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(ChangeSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows add icon dialog when site doesn't have icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(AddSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(false)

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_jetpack_message))
        )
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site has icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = siteIcon

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_permission_message))
        )
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site does not have icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = null

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_add_requires_permission_message))
        )
    }

    @Test
    fun `on site name chosen updates title if network available `() {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository).updateTitle(title)
    }

    @Test
    fun `on site name chosen shows snackbar if network not available `() {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository, never()).updateTitle(any())
        assertThat(snackbars).containsOnly(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
    }

    @Test
    fun `site block url click opens site`() {
        invokeSiteInfoBlockAction(URL_CLICK)

        assertThat(navigationActions).containsOnly(OpenSite(site))
    }

    @Test
    fun `site block switch click opens site picker`() {
        invokeSiteInfoBlockAction(SWITCH_SITE_CLICK)

        assertThat(navigationActions).containsOnly(OpenSitePicker(site))
    }

    @Test
    fun `account avatar url initial value is empty`() {
        assertThat(uiModels).hasSize(1)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `account avatar url value is emitted after refresh`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refreshAccountAvatarUrl()

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().accountAvatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `account avatar url value is emitted after refresh even if new value is the same`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refreshAccountAvatarUrl()
        viewModel.refreshAccountAvatarUrl()

        assertThat(uiModels).hasSize(3)
    }

    @Test
    fun `account avatar url value is emitted after refresh even if new value is empty`() {
        setupAccount(buildAccountWithAvatarUrl(avatarUrl))

        viewModel.refreshAccountAvatarUrl()

        setupAccount(buildAccountWithAvatarUrl(null))

        viewModel.refreshAccountAvatarUrl()

        assertThat(uiModels).hasSize(3)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `account avatar url value is emitted after refresh even if account is null`() {
        setupAccount(null)

        viewModel.refreshAccountAvatarUrl()

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().accountAvatarUrl).isEmpty()
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(OpenMeScreen)
    }

    @Test
    fun `quick actions are not shown when no site is selected`() {
        onSiteChange.postValue(null)

        assertThat(uiModels.last().items).doesNotHaveAnyElementsOfTypes(QuickActionsBlock::class.java)
    }

    @Test
    fun `quick actions does not show pages button when site doesn't have the required capability`() {
        site.hasCapabilityEditPages = false

        onSiteChange.postValue(site)

        val quickActionsBlock = findQuickActionsBlock()

        assertThat(quickActionsBlock).isNotNull
        assertThat(quickActionsBlock?.showPages).isFalse
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is WPCOM`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStatsScreen(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStatsScreen(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenConnectJetpackForStatsScreen(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(StartLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenConnectJetpackForStatsScreen(site))
    }

    @Test
    fun `quick action pages click opens pages screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onPagesClick?.click()

        assertThat(navigationActions).containsOnly(OpenPagesScreen(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onPostsClick?.click()

        assertThat(navigationActions).containsOnly(OpenPostsScreen(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onMediaClick?.click()

        assertThat(navigationActions).containsOnly(OpenMediaScreen(site))
    }

    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(OpenStatsScreen(site))
    }

    private fun setupAccount(account: AccountModel?) = whenever(accountStore.account).thenReturn(account)

    private fun buildAccountWithAvatarUrl(avatarUrl: String?) = AccountModel().apply { this.avatarUrl = avatarUrl }

    private fun findQuickActionsBlock() = uiModels.last().items.find { it is QuickActionsBlock } as QuickActionsBlock?

    private fun invokeSiteInfoBlockAction(action: SiteInfoBlockAction) {
        val argument = when (action) {
            TITLE_CLICK -> 2
            ICON_CLICK -> 3
            URL_CLICK -> 4
            SWITCH_SITE_CLICK -> 5
        }
        var clickAction: ((SiteModel) -> Unit)? = null
        doAnswer {
            clickAction = it.getArgument(argument)
            siteInfoBlock
        }.whenever(siteInfoBlockBuilder).buildSiteInfoBlock(eq(site), any(), any(), any(), any(), any())

        onSiteChange.postValue(site)

        assertThat(clickAction).isNotNull()
        clickAction!!.invoke(site)
    }

    private enum class SiteInfoBlockAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }
}
