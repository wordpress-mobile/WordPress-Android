package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard.IconState
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoCardAction.ICON_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoCardAction.SWITCH_SITE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoCardAction.TITLE_CLICK
import org.wordpress.android.ui.mysite.MySiteViewModelTest.SiteInfoCardAction.URL_CLICK
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ShowRemoveNextStepsDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenQuickStartFullScreenDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.ShowQuickStartDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.mysite.cards.CardsBuilder
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationHandler
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartItemBuilder
import org.wordpress.android.ui.mysite.items.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.STATS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.THEMES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.VIEW_SITE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.UnifiedCommentsListFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider

private const val CARDS_BUILDER_SITE_INFO_TITLE_CLICK_PARAM_POSITION = 5
private const val CARDS_BUILDER_SITE_INFO_ICON_CLICK_PARAM_POSITION = 6
private const val CARDS_BUILDER_SITE_INFO_URL_CLICK_PARAM_POSITION = 7
private const val CARDS_BUILDER_SITE_INFO_SWITCH_SITE_PARAM_POSITION = 8
private const val CARDS_BUILDER_DOMAIN_REGISTRATION_CLICK_PARAM_POSITION = 13
private const val CARDS_BUILDER_QUICK_START_REMOVE_MENU_CLICK_PARAM_POSITION = 14
private const val CARDS_BUILDER_QUICK_START_TASK_TYPE_ITEM_CLICK_PARAM_POSITION = 15

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest : BaseUnitTest() {
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
    @Mock lateinit var quickStartCardBuilder: QuickStartCardBuilder
    @Mock lateinit var scanAndBackupSource: ScanAndBackupSource
    @Mock lateinit var currentAvatarSource: CurrentAvatarSource
    @Mock lateinit var dynamicCardsSource: DynamicCardsSource
    @Mock lateinit var unifiedCommentsListFeatureConfig: UnifiedCommentsListFeatureConfig
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Mock lateinit var snackbarSequencer: SnackbarSequencer
    @Mock lateinit var cardsBuilder: CardsBuilder
    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<UiModel>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val siteLocalId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val emailAddress = "test@email.com"
    private lateinit var site: SiteModel
    private lateinit var siteInfoCard: SiteInfoCard
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onSiteSelected = MutableLiveData<Int>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableLiveData(DomainCreditAvailable(false))
    private val jetpackCapabilities = MutableLiveData(JetpackCapabilities(false, false))
    private val currentAvatar = MutableLiveData(CurrentAvatarUrl(""))
    private val quickStartUpdate = MutableLiveData(QuickStartUpdate())
    private val activeTask = MutableLiveData<QuickStartTask>()
    private val dynamicCards = MutableLiveData(
            DynamicCardsUpdate(
                    cards = listOf(
                            CUSTOMIZE_QUICK_START,
                            GROW_QUICK_START
                    )
            )
    )
    private var removeMenuItemClickAction: (() -> Unit)? = null
    private var quickStartTaskTypeItemClickAction: ((QuickStartTaskType) -> Unit)? = null
    private var dynamicCardMoreClick: ((DynamicCardMenuModel) -> Unit)? = null
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
                taskType = QuickStartTaskType.CUSTOMIZE,
                uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
                completedTasks = emptyList()
        )
    private val dynamicQuickStartTaskCard: QuickStartDynamicCard
        get() = QuickStartDynamicCard(
                CUSTOMIZE_QUICK_START,
                UiStringRes(0),
                emptyList(),
                0,
                0,
                ListItemInteraction.create(
                        DynamicCardMenuModel(CUSTOMIZE_QUICK_START, true),
                        dynamicCardMoreClick as (DynamicCardMenuModel) -> Unit
                )
        )

    private var quickActionsStatsClickAction: (() -> Unit)? = null
    private var quickActionsPagesClickAction: (() -> Unit)? = null
    private var quickActionsPostsClickAction: (() -> Unit)? = null
    private var quickActionsMediaClickAction: (() -> Unit)? = null
    private val quickActionsCard: QuickActionsCard
        get() = QuickActionsCard(
                title = UiStringText(""),
                onStatsClick = ListItemInteraction.create { quickActionsStatsClickAction },
                onPagesClick = ListItemInteraction.create { quickActionsPagesClickAction },
                onPostsClick = ListItemInteraction.create { quickActionsPostsClickAction },
                onMediaClick = ListItemInteraction.create { quickActionsMediaClickAction },
                showPages = site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                showPagesFocusPoint = false,
                showStatsFocusPoint = false
        )

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        onSiteSelected.value = null
        whenever(domainRegistrationHandler.buildSource(any(), any())).thenReturn(isDomainCreditAvailable)
        whenever(scanAndBackupSource.buildSource(any(), any())).thenReturn(jetpackCapabilities)
        whenever(currentAvatarSource.buildSource(any())).thenReturn(currentAvatar)
        whenever(currentAvatarSource.buildSource(any(), any())).thenReturn(currentAvatar)
        whenever(quickStartRepository.buildSource(any(), any())).thenReturn(quickStartUpdate)
        whenever(dynamicCardsSource.buildSource(any(), any())).thenReturn(dynamicCards)
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        whenever(selectedSiteRepository.siteSelected).thenReturn(onSiteSelected)
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        viewModel = MySiteViewModel(
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
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
                quickStartCardBuilder,
                currentAvatarSource,
                dynamicCardsSource,
                unifiedCommentsListFeatureConfig,
                quickStartDynamicCardsFeatureConfig,
                quickStartUtilsWrapper,
                snackbarSequencer,
                cardsBuilder
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
        site.id = siteLocalId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        site.siteId = siteLocalId.toLong()

        setUpCardsBuilder()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    /* SITE STATE */
    @Test
    fun `model is empty with no selected site`() {
        onSiteSelected.value = null
        currentAvatar.value = CurrentAvatarUrl("")

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
    }

    @Test
    fun `model contains header of selected site`() {
        initSelectedSite()

        assertThat(uiModels.last().state).isInstanceOf(SiteSelected::class.java)

        assertThat(getLastItems().first()).isInstanceOf(SiteInfoCard::class.java)
    }

    /* AVATAR */
    @Test
    fun `account avatar url value is emitted and updated from the source`() {
        initSelectedSite()

        currentAvatar.value = CurrentAvatarUrl(avatarUrl)

        assertThat(uiModels.last().accountAvatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(OpenMeScreen)
    }

    /* LOGIN - NAVIGATION TO STATS */
    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    /* EMPTY VIEW */
    @Test
    fun `when no site is selected and screen height is higher than 600 pixels, show empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(600)

        initSelectedSite()
        onSiteSelected.value = null

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
        assertThat((uiModels.last().state as NoSites).shouldShowImage).isTrue
    }

    @Test
    fun `when no site is selected and screen height is lower than 600 pixels, hide empty view image`() {
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(500)

        initSelectedSite()
        onSiteSelected.value = null

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
        assertThat((uiModels.last().state as NoSites).shouldShowImage).isFalse
    }

    /* EMPTY VIEW - ADD SITE */
    @Test
    fun `add new site press is handled correctly`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(AddNewSite(true))
    }

    /* REFRESH */
    @Test
    fun `when refresh is triggered, then update site settings if necessary`() {
        viewModel.refresh()

        verify(selectedSiteRepository).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `when refresh is triggered, then refresh quick start`() {
        viewModel.refresh()

        verify(quickStartRepository).refresh()
    }

    @Test
    fun `when refresh is triggered, then refresh current avatar`() {
        viewModel.refresh()

        verify(currentAvatarSource).refresh()
    }

    @Test
    fun `when clear active quick start task is triggered, then clear active quick start task`() {
        viewModel.clearActiveQuickStartTask()

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `when check and show quick start notice is triggered, then check and show quick start notice`() {
        viewModel.checkAndShowQuickStartNotice()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* SITE INFO CARD */
    @Test
    fun `site info card title click shows snackbar message when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoCardAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site info card title click shows snackbar message when hasCapabilityManageOptions is false`() = test {
        site.hasCapabilityManageOptions = false
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        invokeSiteInfoCardAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(
                        UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint)
                )
        )
    }

    @Test
    fun `site info card title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoCardAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site info card title click shows input dialog when editing allowed`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        invokeSiteInfoCardAction(TITLE_CLICK)

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
    fun `site info card icon click shows change icon dialog when site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(ChangeSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows add icon dialog when site doesn't have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoCardAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(AddSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() =
            test {
                site.hasCapabilityManageOptions = true
                site.hasCapabilityUploadFiles = false
                site.setIsWPCom(false)

                invokeSiteInfoCardAction(ICON_CLICK)

                assertThat(dialogModels).isEmpty()
                assertThat(snackbars).containsOnly(
                        SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_jetpack_message))
                )
            }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_permission_message))
        )
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site does not have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = null

        invokeSiteInfoCardAction(ICON_CLICK)

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
    fun `site info card url click opens site`() = test {
        invokeSiteInfoCardAction(URL_CLICK)

        assertThat(navigationActions).containsOnly(OpenSite(site))
    }

    @Test
    fun `site info card switch click opens site picker`() = test {
        invokeSiteInfoCardAction(SWITCH_SITE_CLICK)

        assertThat(navigationActions).containsOnly(OpenSitePicker(site))
    }

    /* QUICK ACTIONS CARD */
    @Test
    fun `quick actions does not show pages button when site doesn't have the required capability`() {
        site.hasCapabilityEditPages = false

        initSelectedSite()

        val quickActionsCard = findQuickActionsCard()

        assertThat(quickActionsCard).isNotNull
        assertThat(quickActionsCard?.showPages).isFalse
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is WPCOM`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click completes CHECK_STATS task`() {
        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        verify(quickStartRepository).completeTask(CHECK_STATS)
    }

    @Test
    fun `quick action pages click opens pages screen and requests next step of EDIT_HOMEPAGE task`() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).requestNextStepOfTask(EDIT_HOMEPAGE)
        assertThat(navigationActions).containsOnly(OpenPages(site))
    }

    @Test
    fun `quick action pages click opens pages screen and completes REVIEW_PAGES task`() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).completeTask(REVIEW_PAGES)
        assertThat(navigationActions).containsOnly(OpenPages(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        initSelectedSite()

        requireNotNull(quickActionsPostsClickAction).invoke()

        assertThat(navigationActions).containsOnly(OpenPosts(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        initSelectedSite()

        requireNotNull(quickActionsMediaClickAction).invoke()

        assertThat(navigationActions).containsOnly(OpenMedia(site))
    }

    /* DOMAIN REGISTRATION CARD */
    @Test
    fun `domain registration item click opens domain registration`() {
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        findDomainRegistrationCard()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(OpenDomainRegistration(site))
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

    /* QUICK START CARD + DYNAMIC CARD */
    @Test
    fun `hides quick start menu item in quickStartRepository`() {
        val id = CUSTOMIZE_QUICK_START
        viewModel.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Hide(id))

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_HIDE_CARD_TAPPED)
        verify(quickStartRepository).refresh()
    }

    @Test
    fun `removes quick start menu item in quickStartRepository`() {
        val id = CUSTOMIZE_QUICK_START
        viewModel.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Remove(id))

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_REMOVE_CARD_TAPPED)
        verify(quickStartRepository).refresh()
    }

    @Test
    fun `given quick start is not in progress, when site is selected, then quick start card not built`() {
        initSelectedSite(isQuickStartInProgress = false)

        assertThat(findQuickStartCard()).isNull()
    }

    @Test
    fun `given quick start is not in progress, when site is selected, then QS dynamic card not built`() {
        initSelectedSite(isQuickStartInProgress = false)

        assertThat(findQuickStartDynamicCard()).isNull()
    }

    @Test
    fun `given dynamic card disabled + QS in progress, when site is selected, then QS dynamic card not built`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        assertThat(findQuickStartDynamicCard()).isNull()
    }

    @Test
    fun `given dynamic card enabled + quick start in progress, when site is selected, then QS dynamic card built`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = true, isQuickStartInProgress = true)

        assertThat(findQuickStartDynamicCard()).isNotNull
    }

    @Test
    fun `given dynamic card disabled + quick start in progress, when site is selected, then QS card built`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        assertThat(findQuickStartCard()).isNotNull
    }

    @Test
    fun `given dynamic card enabled + quick start in progress, when site is selected, then QS card not built`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = true, isQuickStartInProgress = true)

        assertThat(findQuickStartCard()).isNull()
    }

    @Test
    fun `when quick start task type item is clicked, then quick start full screen dialog is opened`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        assertThat(navigationActions.last()).isInstanceOf(OpenQuickStartFullScreenDialog::class.java)
    }

    @Test
    fun `when quick start task type item is clicked, then quick start active task is cleared`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `given dynamic card disabled, when QS remove menu item is clicked, then remove next steps dialog shown`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(removeMenuItemClickAction).invoke()

        assertThat(dialogModels.last()).isEqualTo(ShowRemoveNextStepsDialog)
    }

    @Test
    fun `when remove next steps dialog negative btn clicked, then QS is not skipped`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository, never()).skipQuickStart()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS is skipped`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository).skipQuickStart()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS repo refreshed`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository).refresh()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS active task cleared`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `when QS fullscreen dialog dismiss is triggered, then quick start repository is refreshed`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onQuickStartFullScreenDialogDismiss()

        verify(quickStartRepository).refresh()
    }

    @Test
    fun `when quick start task is clicked, then task is set as active task`() {
        val task = QuickStartTask.VIEW_SITE
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onQuickStartTaskCardClick(task)

        verify(quickStartRepository).setActiveTask(task)
    }

    /* START/IGNORE QUICK START + QUICK START DIALOG */
    @Test
    fun `given QS dynamic cards cards feature is on, when check and start QS is triggered, then QS starts`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.checkAndStartQuickStart(siteLocalId)

        verify(quickStartRepository).startQuickStart(siteLocalId)
    }

    @Test
    fun `given no selected site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for the site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `when check and start QS is triggered, then QSP is shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).containsExactly(
                ShowQuickStartDialog(
                        R.string.quick_start_dialog_need_help_manage_site_title,
                        R.string.quick_start_dialog_need_help_manage_site_message,
                        R.string.quick_start_dialog_need_help_manage_site_button_positive,
                        R.string.quick_start_dialog_need_help_button_negative
                )
        )
    }

    @Test
    fun `when start QS is triggered, then QS request dialog positive tapped is tracked`() {
        viewModel.startQuickStart()

        verify(analyticsTrackerWrapper).track(QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
    }

    @Test
    fun `when start QS is triggered, then QS starts`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(site.id)

        viewModel.startQuickStart()

        verify(quickStartRepository).startQuickStart(site.id)
    }

    @Test
    fun `when ignore QS is triggered, then QS request dialog negative tapped is tracked`() {
        viewModel.ignoreQuickStart()

        verify(analyticsTrackerWrapper).track(QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    /* ITEM CLICK */
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
    fun `plan item click emits OpenPlan navigation event and completes EXPLORE_PLANS quick task`() {
        invokeItemClickAction(PLAN)

        verify(quickStartRepository).completeTask(EXPLORE_PLANS)
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

        verify(quickStartRepository).completeTask(REVIEW_PAGES)
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
    fun `stats item click completes CHECK_STATS task`() {
        invokeItemClickAction(STATS)

        verify(quickStartRepository).completeTask(CHECK_STATS)
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

    /* ITEM VISIBILITY */
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
                showEnablePostSharingFocusPoint = any(),
                showExplorePlansFocusPoint = any()
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
                showEnablePostSharingFocusPoint = any(),
                showExplorePlansFocusPoint = any()
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
                showEnablePostSharingFocusPoint = any(),
                showExplorePlansFocusPoint = any()
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
                showEnablePostSharingFocusPoint = any(),
                showExplorePlansFocusPoint = any()
        )
    }

    /* ADD SITE ICON DIALOG */
    @Test
    fun `when add site icon dialog +ve btn is clicked, then upload site icon task marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = UPLOAD_SITE_ICON, refreshImmediately = false)
    }

    @Test
    fun `when change site icon dialog +ve btn clicked, then upload site icon task marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = UPLOAD_SITE_ICON, refreshImmediately = false)
    }

    @Test
    fun `when add site icon dialog -ve btn is clicked, then upload site icon task marked complete with refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = UPLOAD_SITE_ICON, refreshImmediately = true)
    }

    @Test
    fun `when change site icon dialog -ve btn is clicked, then upload site icon task marked complete with refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = UPLOAD_SITE_ICON, refreshImmediately = true)
    }

    @Test
    fun `when site icon dialog is dismissed, then upload site icon task is marked complete with refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = UPLOAD_SITE_ICON, refreshImmediately = true)
    }

    @Test
    fun `when add site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        assertThat(navigationActions).containsExactly(OpenMediaPicker(site))
    }

    @Test
    fun `when change site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        assertThat(navigationActions).containsExactly(OpenMediaPicker(site))
    }

    @Test
    fun `when add site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when add site icon dialog is dismissed, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog is dismissed, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* SITE CHOOSER DIALOG */
    @Test
    fun `when site chooser is dismissed, then check and show quick start notice`() {
        viewModel.onSiteNameChooserDismissed()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    private fun findQuickActionsCard() = getLastItems().find { it is QuickActionsCard } as QuickActionsCard?

    private fun findQuickStartCard() = getLastItems().find { it is QuickStartCard } as QuickStartCard?

    private fun findQuickStartDynamicCard() = getLastItems().find { it is DynamicCard } as DynamicCard?

    private fun findDomainRegistrationCard() =
            getLastItems().find { it is DomainRegistrationCard } as DomainRegistrationCard?

    private fun findSiteInfoCard() =
            getLastItems().find { it is SiteInfoCard } as SiteInfoCard?

    private fun getLastItems() = (uiModels.last().state as SiteSelected).cardAndItems

    private suspend fun invokeSiteInfoCardAction(action: SiteInfoCardAction) {
        onSiteChange.value = site
        onSiteSelected.value = siteLocalId
        while (uiModels.last().state is NoSites) {
            delay(100)
        }
        val siteInfoCard = findSiteInfoCard()!!
        when (action) {
            TITLE_CLICK -> siteInfoCard.onTitleClick!!.click()
            ICON_CLICK -> siteInfoCard.onIconClick.click()
            URL_CLICK -> siteInfoCard.onUrlClick.click()
            SWITCH_SITE_CLICK -> siteInfoCard.onSwitchSiteClick.click()
        }
    }

    private fun invokeItemClickAction(action: ListItemAction) {
        var clickAction: ((ListItemAction) -> Unit)? = null
        doAnswer {
            clickAction = it.getArgument(1)
            listOf<MySiteCardAndItem>()
        }.whenever(siteItemsBuilder).buildSiteItems(eq(site), any(), any(), any(), any(), any(), any())

        initSelectedSite()

        assertThat(clickAction).isNotNull
        clickAction!!.invoke(action)
    }

    private fun initSelectedSite(
        isQuickStartDynamicCardEnabled: Boolean = false,
        isQuickStartInProgress: Boolean = false
    ) {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(isQuickStartDynamicCardEnabled)
        doAnswer {
            dynamicCardMoreClick = (it.getArgument(2) as (DynamicCardMenuModel) -> Unit)
            dynamicQuickStartTaskCard
        }.whenever(quickStartItemBuilder).build(any(), anyOrNull(), any(), any())
        quickStartUpdate.value = QuickStartUpdate(
                categories = if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList()
        )
        onSiteSelected.value = siteLocalId
        onSiteChange.value = site
    }

    private enum class SiteInfoCardAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }

    private fun setUpCardsBuilder() {
        doAnswer {
            siteInfoCard = initSiteInfoCard(it)
            val quickActionsCard = initQuickActionsCard(it)
            val domainRegistrationCard = initDomainRegistrationCard(it)
            val quickStartCard = initQuickStartCard(it)
            listOf<MySiteCardAndItem>(siteInfoCard, quickActionsCard, domainRegistrationCard, quickStartCard)
        }.whenever(cardsBuilder).build(
                site = eq(site),
                showSiteIconProgressBar = any(),
                activeTask = anyOrNull(),
                isDomainCreditAvailable = anyBoolean(),
                quickStartCategories = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                quickActionStatsClick = any(),
                quickActionPagesClick = any(),
                quickActionPostsClick = any(),
                quickActionMediaClick = any(),
                domainRegistrationClick = any(),
                onQuickStartBlockRemoveMenuItemClick = any(),
                onQuickStartTaskTypeItemClick = any()
        )
    }

    private fun initSiteInfoCard(mockInvocation: InvocationOnMock) = SiteInfoCard(
            title = siteName,
            url = siteUrl,
            iconState = IconState.Visible(siteIcon),
            showTitleFocusPoint = false,
            showIconFocusPoint = false,
            onTitleClick = ListItemInteraction.create {
                (mockInvocation.getArgument(CARDS_BUILDER_SITE_INFO_TITLE_CLICK_PARAM_POSITION) as () -> Unit).invoke()
            },
            onIconClick = ListItemInteraction.create {
                (mockInvocation.getArgument(CARDS_BUILDER_SITE_INFO_ICON_CLICK_PARAM_POSITION) as () -> Unit).invoke()
            },
            onUrlClick = ListItemInteraction.create {
                (mockInvocation.getArgument(CARDS_BUILDER_SITE_INFO_URL_CLICK_PARAM_POSITION) as () -> Unit).invoke()
            },
            onSwitchSiteClick = ListItemInteraction.create {
                (mockInvocation.getArgument(CARDS_BUILDER_SITE_INFO_SWITCH_SITE_PARAM_POSITION) as () -> Unit).invoke()
            }
    )

    private fun initQuickActionsCard(mockInvocation: InvocationOnMock): QuickActionsCard {
        quickActionsStatsClickAction = mockInvocation.getArgument(9)
        quickActionsPagesClickAction = mockInvocation.getArgument(10)
        quickActionsPostsClickAction = mockInvocation.getArgument(11)
        quickActionsMediaClickAction = mockInvocation.getArgument(12)
        return QuickActionsCard(
                title = UiStringText(""),
                onStatsClick = ListItemInteraction.create { (quickActionsStatsClickAction as () -> Unit).invoke() },
                onPagesClick = ListItemInteraction.create { (quickActionsPagesClickAction as () -> Unit).invoke() },
                onPostsClick = ListItemInteraction.create { (quickActionsPostsClickAction as () -> Unit).invoke() },
                onMediaClick = ListItemInteraction.create { (quickActionsMediaClickAction as () -> Unit).invoke() },
                showPages = site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                showPagesFocusPoint = false,
                showStatsFocusPoint = false
        )
    }

    private fun initDomainRegistrationCard(mockInvocation: InvocationOnMock) = DomainRegistrationCard(
            ListItemInteraction.create {
                (mockInvocation.getArgument(CARDS_BUILDER_DOMAIN_REGISTRATION_CLICK_PARAM_POSITION) as () -> Unit)
                        .invoke()
            }
    )

    private fun initQuickStartCard(mockInvocation: InvocationOnMock): QuickStartCard {
        removeMenuItemClickAction = mockInvocation.getArgument(
                CARDS_BUILDER_QUICK_START_REMOVE_MENU_CLICK_PARAM_POSITION
        ) as () -> Unit
        quickStartTaskTypeItemClickAction = mockInvocation.getArgument(
                CARDS_BUILDER_QUICK_START_TASK_TYPE_ITEM_CLICK_PARAM_POSITION
        ) as ((QuickStartTaskType) -> Unit)
        return QuickStartCard(
                title = UiStringText(""),
                onRemoveMenuItemClick = ListItemInteraction.create {
                    (removeMenuItemClickAction as () -> Unit).invoke()
                },
                taskTypeItems = listOf(
                        QuickStartTaskTypeItem(
                                quickStartTaskType = mock(),
                                title = UiStringText(""),
                                titleEnabled = true,
                                subtitle = UiStringText(""),
                                strikeThroughTitle = false,
                                progressColor = 0,
                                progress = 0,
                                onClick = ListItemInteraction.create(
                                        mock(),
                                        (quickStartTaskTypeItemClickAction as ((QuickStartTaskType) -> Unit))
                                )
                        )
                )
        )
    }
}
