package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_PROMPT_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.mysite.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.ListItemAction.STATS
import org.wordpress.android.ui.mysite.ListItemAction.THEMES
import org.wordpress.android.ui.mysite.ListItemAction.VIEW_SITE
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.ICON_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.SWITCH_SITE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.TITLE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.URL_CLICK
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartModel
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider

@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest : BaseUnitTest() {
    @Mock lateinit var siteInfoBlockBuilder: SiteInfoBlockBuilder
    @Mock lateinit var siteItemsBuilder: SiteItemsBuilder
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var siteIconUploadHandler: SiteIconUploadHandler
    @Mock lateinit var siteStoriesHandler: SiteStoriesHandler
    @Mock lateinit var domainRegistrationHandler: DomainRegistrationHandler
    @Mock lateinit var backupScreenFeatureConfig: BackupScreenFeatureConfig
    @Mock lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock lateinit var scanScreenFeatureConfig: ScanScreenFeatureConfig
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Mock lateinit var quickStartRepository: QuickStartRepository
    @Mock lateinit var quickStartItemBuilder: QuickStartItemBuilder
    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<UiModel>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val siteId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val emailAddress = "test@email.com"
    private lateinit var site: SiteModel
    private lateinit var siteInfoBlock: SiteInfoBlock
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableLiveData<Boolean>()
    private val quickStartModel = MutableLiveData<QuickStartModel>()

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        isDomainCreditAvailable.value = null
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
        whenever(domainRegistrationHandler.isDomainCreditAvailable).thenReturn(isDomainCreditAvailable)
        whenever(quickStartRepository.quickStartModel).thenReturn(quickStartModel)
        viewModel = MySiteViewModel(
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
                siteInfoBlockBuilder,
                siteItemsBuilder,
                accountStore,
                selectedSiteRepository,
                wpMediaUtilsWrapper,
                mediaUtilsWrapper,
                fluxCUtilsWrapper,
                contextProvider,
                siteIconUploadHandler,
                siteStoriesHandler,
                domainRegistrationHandler,
                backupScreenFeatureConfig,
                displayUtilsWrapper,
                jetpackCapabilitiesUseCase,
                scanScreenFeatureConfig,
                quickStartRepository,
                quickStartItemBuilder
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
        site.id = siteId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        siteInfoBlock = SiteInfoBlock(
                title = siteName,
                url = siteUrl,
                iconState = IconState.Visible(siteIcon),
                showTitleFocusPoint = false,
                showIconFocusPoint = false,
                onTitleClick = null,
                onIconClick = mock(),
                onUrlClick = mock(),
                onSwitchSiteClick = mock()
        )
        whenever(siteInfoBlockBuilder.buildSiteInfoBlock(
                site = eq(site),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = any(),
                showUploadSiteIconFocusPoint = any()
        )).thenReturn(
                siteInfoBlock
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `model is empty with no selected site`() {
        onSiteChange.postValue(null)

        assertThat(uiModels).hasSize(2)
        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
    }

    @Test
    fun `model contains header of selected site`() {
        onSiteChange.postValue(site)

        assertThat(uiModels).hasSize(3)
        val state = uiModels.last().state as SiteSelected
        assertThat(state.showFabFocusPoint).isFalse()

        assertThat(getLastItems()).hasSize(2)
        assertThat(getLastItems().first()).isInstanceOf(SiteInfoBlock::class.java)
    }

    @Test
    fun `model shows focus point on FAB when active task is publish post`() {
        onSiteChange.postValue(site)
        quickStartModel.value = QuickStartModel(PUBLISH_POST)

        val state = uiModels.last().state as SiteSelected
        assertThat(state.showFabFocusPoint).isTrue()
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
    fun `passes active UPDATE_SITE_TITLE into site info block builder`() {
        onSiteChange.postValue(site)

        whenever(siteInfoBlockBuilder.buildSiteInfoBlock(
                site = eq(site),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = eq(true),
                showUploadSiteIconFocusPoint = eq(false)
        )).thenReturn(
                siteInfoBlock.copy(showTitleFocusPoint = true)
        )

        quickStartModel.value = QuickStartModel(UPDATE_SITE_TITLE, listOf())

        assertThat(findSiteInfoBlock()!!.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `passes active UPLOAD_SITE_ICON into site info block builder`() {
        onSiteChange.postValue(site)

        whenever(siteInfoBlockBuilder.buildSiteInfoBlock(
                site = eq(site),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = eq(false),
                showUploadSiteIconFocusPoint = eq(true)
        )).thenReturn(
                siteInfoBlock.copy(showIconFocusPoint = true)
        )

        quickStartModel.value = QuickStartModel(UPLOAD_SITE_ICON, listOf())

        assertThat(findSiteInfoBlock()!!.showIconFocusPoint).isTrue()
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
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(OpenMeScreen)
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

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action pages click opens pages screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onPagesClick?.click()

        assertThat(navigationActions).containsOnly(OpenPages(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onPostsClick?.click()

        assertThat(navigationActions).containsOnly(OpenPosts(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        onSiteChange.postValue(site)

        findQuickActionsBlock()?.onMediaClick?.click()

        assertThat(navigationActions).containsOnly(OpenMedia(site))
    }

    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `activity item click emits OpenActivity navigation event`() {
        invokeItemClickAction(ACTIVITY_LOG)

        assertThat(navigationActions).containsExactly(OpenActivityLog(site))
    }

    @Test
    fun `scan item click emits OpenScan navigation event`() {
        invokeItemClickAction(SCAN)

        assertThat(navigationActions).containsExactly(OpenScan(site))
    }

    @Test
    fun `plan item click emits OpenPlan navigation event`() {
        invokeItemClickAction(PLAN)

        assertThat(navigationActions).containsExactly(OpenPlan(site))
    }

    @Test
    fun `posts item click emits OpenPosts navigation event`() {
        invokeItemClickAction(POSTS)

        assertThat(navigationActions).containsExactly(OpenPosts(site))
    }

    @Test
    fun `pages item click emits OpenPages navigation event`() {
        invokeItemClickAction(PAGES)

        assertThat(navigationActions).containsExactly(OpenPages(site))
    }

    @Test
    fun `admin item click emits OpenAdmin navigation event`() {
        invokeItemClickAction(ADMIN)

        assertThat(navigationActions).containsExactly(OpenAdmin(site))
    }

    @Test
    fun `sharing item click emits OpenSharing navigation event`() {
        invokeItemClickAction(SHARING)

        assertThat(navigationActions).containsExactly(OpenSharing(site))
    }

    @Test
    fun `site settings item click emits OpenSiteSettings navigation event`() {
        invokeItemClickAction(SITE_SETTINGS)

        assertThat(navigationActions).containsExactly(OpenSiteSettings(site))
    }

    @Test
    fun `themes item click emits OpenThemes navigation event`() {
        invokeItemClickAction(THEMES)

        assertThat(navigationActions).containsExactly(OpenThemes(site))
    }

    @Test
    fun `plugins item click emits OpenPlugins navigation event`() {
        invokeItemClickAction(PLUGINS)

        assertThat(navigationActions).containsExactly(OpenPlugins(site))
    }

    @Test
    fun `media item click emits OpenMedia navigation event`() {
        invokeItemClickAction(MEDIA)

        assertThat(navigationActions).containsExactly(OpenMedia(site))
    }

    @Test
    fun `comments item click emits OpenMedia navigation event`() {
        invokeItemClickAction(COMMENTS)

        assertThat(navigationActions).containsExactly(OpenComments(site))
    }

    @Test
    fun `view site item click emits OpenSite navigation event`() {
        invokeItemClickAction(VIEW_SITE)

        assertThat(navigationActions).containsExactly(OpenSite(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is WPCom and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsWPCom(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(OpenStats(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is Jetpack and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsJetpackConnected(true)
        site.setIsJetpackInstalled(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(OpenStats(site))
    }

    @Test
    fun `stats item click emits StartWPComLoginForJetpackStats if site is Jetpack and doesn't have access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `stats item click emits ConnectJetpackForStats if neither Jetpack, nor WPCom and no access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(false)
        site.setIsWPCom(false)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(ConnectJetpackForStats(site))
    }

    @Test
    fun `domain registration item click opens domain registration`() {
        onSiteChange.postValue(site)
        isDomainCreditAvailable.postValue(true)

        findDomainRegistrationBlock()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(OpenDomainRegistration(site))
    }

    @Test
    fun `correct event is tracked when domain registration item is shown`() {
        onSiteChange.postValue(site)
        isDomainCreditAvailable.postValue(true)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_PROMPT_SHOWN)
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result without email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(null)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringRes(R.string.my_site_verify_your_email_without_email)

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result with email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(emailAddress)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringResWithParams(R.string.my_site_verify_your_email, listOf(UiStringText(emailAddress)))

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `jetpack menu visibility requested, when selected site changes and scanScreenFeatureFlag is enabled`() = test {
        whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = false)
        )

        onSiteChange.postValue(site)

        verify(jetpackCapabilitiesUseCase).getJetpackPurchasedProducts(site.siteId)
    }

    @Test
    fun `jetpack menu visibility requested, when selected site changes and backupScreenFeatureFlag is enabled`() =
            test {
        whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = false)
        )

        onSiteChange.postValue(site)

        verify(jetpackCapabilitiesUseCase).getJetpackPurchasedProducts(site.siteId)
    }

    @Test
    fun `jetpack menu visibility not requested, when scan and backup screen feature flags are off`() = test {
        whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(false)
        whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(false)

        onSiteChange.postValue(site)

        verify(jetpackCapabilitiesUseCase, never()).getJetpackPurchasedProducts(site.siteId)
    }

    @Test
    fun `backup menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = false)
        )

        onSiteChange.postValue(site)

        verify(siteItemsBuilder, times(2)).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = eq(false),
                isScanAvailable = any(),
                showViewSiteFocusPoint = eq(false)
        )
    }

    @Test
    fun `scan menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = false)
        )

        onSiteChange.postValue(site)

        verify(siteItemsBuilder, times(2)).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = any(),
                isScanAvailable = eq(false),
                showViewSiteFocusPoint = any()
        )
    }

    @Test
    fun `scan menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = false)
        )

        onSiteChange.postValue(site)

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = any(),
                isScanAvailable = eq(true),
                showViewSiteFocusPoint = eq(false)
        )
    }

    @Test
    fun `backup menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong())).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = true)
        )

        onSiteChange.postValue(site)

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = eq(true),
                isScanAvailable = any(),
                showViewSiteFocusPoint = any()
        )
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

    @Test
    fun `add new site press is handled correctly`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(AddNewSite(true))
    }

    private fun setupAccount(account: AccountModel?) = whenever(accountStore.account).thenReturn(account)

    private fun buildAccountWithAvatarUrl(avatarUrl: String?) = AccountModel().apply { this.avatarUrl = avatarUrl }

    private fun findQuickActionsBlock() = getLastItems().find { it is QuickActionsBlock } as QuickActionsBlock?

    private fun findDomainRegistrationBlock() =
            getLastItems().find { it is DomainRegistrationBlock } as DomainRegistrationBlock?

    private fun findSiteInfoBlock() =
            getLastItems().find { it is SiteInfoBlock } as SiteInfoBlock?

    private fun getLastItems() = (uiModels.last().state as SiteSelected).items

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
        }.whenever(siteInfoBlockBuilder).buildSiteInfoBlock(
                site = eq(site),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = any(),
                showUploadSiteIconFocusPoint = any()
        )

        onSiteChange.postValue(site)

        assertThat(clickAction).isNotNull
        clickAction!!.invoke(site)
    }

    private fun invokeItemClickAction(action: ListItemAction) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        var clickAction: ((ListItemAction) -> Unit)? = null
        doAnswer {
            clickAction = it.getArgument(1)
            listOf<MySiteItem>()
        }.whenever(siteItemsBuilder).buildSiteItems(eq(site), any(), any(), any(), any())

        onSiteChange.postValue(site)

        assertThat(clickAction).isNotNull
        clickAction!!.invoke(action)
    }

    private enum class SiteInfoBlockAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }
}
