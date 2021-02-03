package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.test
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
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.ICON_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.SWITCH_SITE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.TITLE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoBlockAction.URL_CLICK
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
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
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
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Mock lateinit var quickStartRepository: QuickStartRepository
    @Mock lateinit var quickStartItemBuilder: QuickStartItemBuilder
    @Mock lateinit var scanAndBackupSource: ScanAndBackupSource
    @Mock lateinit var currentAvatarSource: CurrentAvatarSource
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
    private val onSiteSelected = MutableLiveData<Int>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableStateFlow(DomainCreditAvailable(false))
    private val jetpackCapabilities = MutableStateFlow(JetpackCapabilities(false, false))
    private val currentAvatar = MutableStateFlow(CurrentAvatarUrl(""))
    private val quickStartUpdate = MutableStateFlow(QuickStartUpdate())

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        onSiteSelected.value = null
        whenever(domainRegistrationHandler.buildSource(any())).thenReturn(isDomainCreditAvailable)
        whenever(scanAndBackupSource.buildSource(any())).thenReturn(jetpackCapabilities)
        whenever(currentAvatarSource.buildSource()).thenReturn(currentAvatar)
        whenever(currentAvatarSource.buildSource(any())).thenReturn(currentAvatar)
        whenever(quickStartRepository.buildSource(any())).thenReturn(quickStartUpdate)
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        whenever(selectedSiteRepository.siteSelected).thenReturn(onSiteSelected)
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
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
                scanAndBackupSource,
                displayUtilsWrapper,
                quickStartRepository,
                quickStartItemBuilder,
                currentAvatarSource
        )
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        launch(Dispatchers.Default) {
            viewModel.uiModel.observeForever {
                uiModels.add(it)
            }
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
                onTitleClick = mock(),
                onIconClick = mock(),
                onUrlClick = mock(),
                onSwitchSiteClick = mock()
        )

        doAnswer {
            siteInfoBlock = siteInfoBlock.copy(
                    onTitleClick = ListItemInteraction.create { (it.getArgument(2) as () -> Unit).invoke() },
                    onIconClick = ListItemInteraction.create { (it.getArgument(3) as () -> Unit).invoke() },
                    onUrlClick = ListItemInteraction.create { (it.getArgument(4) as () -> Unit).invoke() },
                    onSwitchSiteClick = ListItemInteraction.create {
                        (it.getArgument(5) as () -> Unit).invoke()
                    }
            )
            siteInfoBlock
        }.whenever(siteInfoBlockBuilder).buildSiteInfoBlock(
                site = any(),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = any(),
                showUploadSiteIconFocusPoint = any()
        )
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `model is empty with no selected site`() {
        onSiteSelected.value = null
        currentAvatar.value = CurrentAvatarUrl("")

        assertThat(uiModels).hasSize(1)
        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
    }

    @Test
    fun `model is contains header of selected site`() {
        initSelectedSite()

        assertThat(uiModels).hasSize(4)
        assertThat(uiModels.last().state).isInstanceOf(State.SiteSelected::class.java)

        assertThat(getLastItems()).hasSize(2)
        assertThat(getLastItems().first()).isInstanceOf(SiteInfoBlock::class.java)
    }

    @Test
    fun `site block title click shows snackbar message when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site block title click shows snackbar message when hasCapabilityManageOptions is false`() = test {
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
    fun `site block title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site block title click shows input dialog when editing allowed`() = test {
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
    fun `site block icon click shows change icon dialog when site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(ChangeSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows add icon dialog when site doesn't have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(AddSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() = test {
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
    fun `site block icon click shows snackbar when upload files not allowed and site has icon`() = test {
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
    fun `site block icon click shows snackbar when upload files not allowed and site does not have icon`() = test {
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
    fun `on site name chosen updates title if network available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository).updateTitle(title)
    }

    @Test
    fun `on site name chosen shows snackbar if network not available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository, never()).updateTitle(any())
        assertThat(snackbars).containsOnly(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
    }

    @Test
    fun `site block url click opens site`() = test {
        invokeSiteInfoBlockAction(URL_CLICK)

        assertThat(navigationActions).containsOnly(OpenSite(site))
    }

    @Test
    fun `site block switch click opens site picker`() = test {
        invokeSiteInfoBlockAction(SWITCH_SITE_CLICK)

        assertThat(navigationActions).containsOnly(OpenSitePicker(site))
    }

    @Test
    fun `passes active UPDATE_SITE_TITLE into site info block builder`() = test {
        initSelectedSite()

        whenever(
                siteInfoBlockBuilder.buildSiteInfoBlock(
                        site = eq(site),
                        showSiteIconProgressBar = any(),
                        titleClick = any(),
                        iconClick = any(),
                        urlClick = any(),
                        switchSiteClick = any(),
                        showUpdateSiteTitleFocusPoint = any(),
                        showUploadSiteIconFocusPoint = any()
                )
        ).thenReturn(
                siteInfoBlock.copy(showTitleFocusPoint = true)
        )

        quickStartUpdate.value = QuickStartUpdate(UPDATE_SITE_TITLE, listOf())

        assertThat(findSiteInfoBlock()!!.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `passes active UPLOAD_SITE_ICON into site info block builder`() {
        initSelectedSite()

        whenever(
                siteInfoBlockBuilder.buildSiteInfoBlock(
                        site = eq(site),
                        showSiteIconProgressBar = any(),
                        titleClick = any(),
                        iconClick = any(),
                        urlClick = any(),
                        switchSiteClick = any(),
                        showUpdateSiteTitleFocusPoint = eq(false),
                        showUploadSiteIconFocusPoint = eq(true)
                )
        ).thenReturn(
                siteInfoBlock.copy(showIconFocusPoint = true)
        )

        quickStartUpdate.value = QuickStartUpdate(UPLOAD_SITE_ICON, listOf())

        assertThat(findSiteInfoBlock()!!.showIconFocusPoint).isTrue()
    }

    @Test
    fun `account avatar url value is emitted and updated from the source`() {
        initSelectedSite()

        currentAvatar.value = CurrentAvatarUrl(avatarUrl)

        assertThat(uiModels).hasSize(5)
        assertThat(uiModels.last().accountAvatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(OpenMeScreen)
    }

    @Test
    fun `quick actions does not show pages button when site doesn't have the required capability`() {
        site.hasCapabilityEditPages = false

        initSelectedSite()

        val quickActionsBlock = findQuickActionsBlock()

        assertThat(quickActionsBlock).isNotNull
        assertThat(quickActionsBlock?.showPages).isFalse
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is WPCOM`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        initSelectedSite()

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action pages click opens pages screen and requests next step of EDIT_HOMEPAGE task`() {
        quickStartUpdate.value = QuickStartUpdate(EDIT_HOMEPAGE, listOf())
        initSelectedSite()

        findQuickActionsBlock()?.onPagesClick?.click()

        verify(quickStartRepository).requestNextStepOfTask(EDIT_HOMEPAGE)
        assertThat(navigationActions).containsOnly(OpenPages(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        initSelectedSite()

        findQuickActionsBlock()?.onPostsClick?.click()

        assertThat(navigationActions).containsOnly(OpenPosts(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        initSelectedSite()

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
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        findDomainRegistrationBlock()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(OpenDomainRegistration(site))
    }

    @Test
    fun `correct event is tracked when domain registration item is shown`() = test {
        onSiteSelected.value = siteId
        onSiteChange.value = site
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        delay(1000)

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
    fun `backup menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        verify(siteItemsBuilder, times(1)).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = eq(false),
                isScanAvailable = any(),
                showViewSiteFocusPoint = eq(false),
                showEnablePostSharingFocusPoint = any()
        )
    }

    @Test
    fun `scan menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        verify(siteItemsBuilder, times(1)).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = any(),
                isScanAvailable = eq(false),
                showViewSiteFocusPoint = any(),
                showEnablePostSharingFocusPoint = any()
        )
    }

    @Test
    fun `scan menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = true, backupAvailable = false)

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = any(),
                isScanAvailable = eq(true),
                showViewSiteFocusPoint = eq(false),
                showEnablePostSharingFocusPoint = any()
        )
    }

    @Test
    fun `backup menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = true)

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = eq(true),
                isScanAvailable = any(),
                showViewSiteFocusPoint = any(),
                showEnablePostSharingFocusPoint = any()
        )
    }

    @Test
    fun `when no site is selected and screen height is higher than 600 pixels, show empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(600)

        onSiteSelected.value = siteId

        assertThat(uiModels.last().state).isInstanceOf(State.NoSites::class.java)
        assertThat((uiModels.last().state as State.NoSites).shouldShowImage).isTrue
    }

    @Test
    fun `when no site is selected and screen height is lower than 600 pixels, hide empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(500)

        onSiteSelected.value = siteId

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

    private fun getLastItems() = (uiModels.last().state as State.SiteSelected).items

    private suspend fun invokeSiteInfoBlockAction(action: SiteInfoBlockAction) {
        onSiteChange.value = site
        onSiteSelected.value = siteId
        while (uiModels.last().state is NoSites) {
            delay(100)
        }
        val siteInfoBlock = findSiteInfoBlock()!!
        when (action) {
            TITLE_CLICK -> siteInfoBlock.onTitleClick!!.click()
            ICON_CLICK -> siteInfoBlock.onIconClick.click()
            URL_CLICK -> siteInfoBlock.onUrlClick.click()
            SWITCH_SITE_CLICK -> siteInfoBlock.onSwitchSiteClick.click()
        }
    }

    private fun invokeItemClickAction(action: ListItemAction) {
        var clickAction: ((ListItemAction) -> Unit)? = null
        doAnswer {
            clickAction = it.getArgument(1)
            listOf<MySiteItem>()
        }.whenever(siteItemsBuilder).buildSiteItems(eq(site), any(), any(), any(), any(), any())

        initSelectedSite()

        assertThat(clickAction).isNotNull
        clickAction!!.invoke(action)
    }

    private fun initSelectedSite() {
        onSiteSelected.value = siteId
        onSiteChange.value = site
    }

    private enum class SiteInfoBlockAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }
}
