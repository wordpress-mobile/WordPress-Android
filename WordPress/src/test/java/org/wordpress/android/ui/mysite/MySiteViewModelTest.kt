package org.wordpress.android.ui.mysite

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.GetShowJetpackFullPluginInstallOnboardingUseCase
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewModelSlice
import org.wordpress.android.ui.mysite.items.DashboardItemsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import java.util.Date

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var siteIconUploadHandler: SiteIconUploadHandler

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var homePageDataLoader: HomePageDataLoader

    @Mock
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper

    @Mock
    lateinit var snackbarSequencer: SnackbarSequencer

    @Mock
    lateinit var landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig

    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var getShowJetpackFullPluginInstallOnboardingUseCase: GetShowJetpackFullPluginInstallOnboardingUseCase

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var quickStartType: QuickStartType

    @Mock
    lateinit var quickStartTracker: QuickStartTracker

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper

    @Mock
    lateinit var siteInfoHeaderCardViewModelSlice: SiteInfoHeaderCardViewModelSlice

    @Mock
    lateinit var accountDataViewModelSlice: AccountDataViewModelSlice

    @Mock
    lateinit var dashboardCardsViewModelSlice: DashboardCardsViewModelSlice

    @Mock
    lateinit var dashboardItemsViewModelSlice: DashboardItemsViewModelSlice


    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<MySiteViewModel.State>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var showSwipeRefreshLayout: MutableList<Boolean>
    private val siteLocalId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val localHomepageId = 1
    private lateinit var site: SiteModel
    private lateinit var homepage: PageModel
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onSiteSelected = MutableLiveData<Int>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val selectedSite = MediatorLiveData<SelectedSite>()

    private val currentAvatar = MutableLiveData(AccountData("",""))
    private val quickStartUpdate = MutableLiveData(QuickStartUpdate())
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
            taskType = QuickStartTaskType.CUSTOMIZE,
            uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
            completedTasks = emptyList()
        )

    @Suppress("LongMethod")
    @Before
    fun setUp() {
        init()
    }

    @Suppress("LongMethod")
    fun init() = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        onSiteSelected.value = null
        selectedSite.value = null
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)

        whenever(siteInfoHeaderCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(accountDataViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(dashboardCardsViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(dashboardItemsViewModelSlice.uiModel).thenReturn(MutableLiveData())

        viewModel = MySiteViewModel(
            testDispatcher(),
            testDispatcher(),
            analyticsTrackerWrapper,
            accountStore,
            selectedSiteRepository,
            siteIconUploadHandler,
            quickStartRepository,
            homePageDataLoader,
            quickStartUtilsWrapper,
            snackbarSequencer,
            landOnTheEditorFeatureConfig,
            buildConfigWrapper,
            appPrefsWrapper,
            quickStartTracker,
            dispatcher,
            jetpackFeatureRemovalOverlayUtil,
            getShowJetpackFullPluginInstallOnboardingUseCase,
            jetpackFeatureRemovalPhaseHelper,
            wpJetpackIndividualPluginHelper,
            siteInfoHeaderCardViewModelSlice,
            accountDataViewModelSlice,
            dashboardCardsViewModelSlice,
            dashboardItemsViewModelSlice
        )
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        showSwipeRefreshLayout = mutableListOf()
        launch(testDispatcher()) {
            viewModel.uiModel.observeForever {
                uiModels.add(it)
            }
        }
        viewModel.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
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

        homepage = PageModel(PostModel(), site, localHomepageId, "home", PUBLISHED, Date(), false, 0L, null, 0L)

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(homePageDataLoader.loadHomepage(site)).thenReturn(homepage)
    }

    /* SITE STATE */

    @Test
    fun `model is empty with no selected site`() {
        onSiteSelected.value = null
        currentAvatar.value = AccountData("","")

        assertThat(uiModels.last()).isInstanceOf(NoSites::class.java)
    }

    @Test
    fun `when selected site is changed, then reset shown tracker is called`() = test {
        initSelectedSite()

        viewModel.onSitePicked()

        verify(dashboardCardsViewModelSlice, atLeastOnce()).resetShownTracker()
        verify(dashboardItemsViewModelSlice, atLeastOnce()).resetShownTracker()
    }


    @Test
    fun `when selected site is changed, then clear ui model value is called`() = test {
        initSelectedSite()

        viewModel.onSitePicked()

        verify(dashboardCardsViewModelSlice, atLeastOnce()).clearValue()
        verify(dashboardItemsViewModelSlice, atLeastOnce()).clearValue()
    }

    @Test
    fun `given jp app, when selected site is changed, then dashboard cards are fetched`() = test {
        initSelectedSite(isJetpackApp = true)

        viewModel.onSitePicked()

        verify(dashboardCardsViewModelSlice, atLeastOnce()).buildCards(site)
    }

    @Test
    fun `given not jp app, when selected site is changed, then site items are fetched`() = test {
        initSelectedSite()

        viewModel.onSitePicked()

        verify(dashboardItemsViewModelSlice, atLeastOnce()).buildItems(site)
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenMeScreen)
    }

    /* LOGIN - NAVIGATION TO STATS */

    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    /* EMPTY VIEW - ADD SITE */
    @Test
    fun `given empty site view, when add new site is tapped, then navigated to AddNewSite`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(
            SiteNavigationAction.AddNewSite(
                true,
                SiteCreationSource.MY_SITE_NO_SITES
            )
        )
    }

    /* ON RESUME */
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

    /* START/IGNORE QUICK START + QUICK START DIALOG */
    @Test
    fun `given no selected site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = false)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for new site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = true)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for existing site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = false)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given new site, when check and start QS is triggered, then QSP is shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(false, isNewSite = true)

        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.ShowQuickStartDialog(
                R.string.quick_start_dialog_need_help_manage_site_title,
                R.string.quick_start_dialog_need_help_manage_site_message,
                R.string.quick_start_dialog_need_help_manage_site_button_positive,
                R.string.quick_start_dialog_need_help_button_negative,
                true
            )
        )
    }

    @Test
    fun `given existing site, when check and start QS is triggered, then QSP is shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(false, isNewSite = false)

        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.ShowQuickStartDialog(
                R.string.quick_start_dialog_need_help_manage_site_title,
                R.string.quick_start_dialog_need_help_manage_site_message,
                R.string.quick_start_dialog_need_help_manage_site_button_positive,
                R.string.quick_start_dialog_need_help_button_negative,
                false
            )
        )
    }

    @Test
    fun `when start QS is triggered, then QS request dialog positive tapped is tracked`() {
        viewModel.startQuickStart()

        verify(quickStartTracker).track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
    }

    @Test
    fun `when start QS is triggered, then QS starts`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(site.id)

        viewModel.startQuickStart()

        verify(quickStartUtilsWrapper)
            .startQuickStart(site.id, false, quickStartRepository.quickStartType, quickStartTracker)
        verify(dashboardCardsViewModelSlice).startQuickStart(site)
    }

    @Test
    fun `when ignore QS is triggered, then QS request dialog negative tapped is tracked`() {
        viewModel.ignoreQuickStart()

        verify(quickStartTracker).track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    /* DASHBOARD BLOGGING PROMPT */
    @Test
    fun `when blogging prompt answer is uploaded, refresh prompt card`() = test {
        initSelectedSite()

        val promptAnswerPost = PostModel().apply { answeredPromptId = 1 }

        val postUploadedEvent = PostStore.OnPostUploaded(promptAnswerPost, true)

        viewModel.onPostUploaded(postUploadedEvent)

        verify(dashboardCardsViewModelSlice).refreshBloggingPrompt()
    }

    @Test
    fun `when non blogging prompt answer is uploaded, prompt card is not refreshed`() = test {
        initSelectedSite()

        val promptAnswerPost = PostModel().apply { answeredPromptId = 0 }

        val postUploadedEvent = PostStore.OnPostUploaded(promptAnswerPost, true)

        viewModel.onPostUploaded(postUploadedEvent)

        verify(dashboardCardsViewModelSlice, never()).refreshBloggingPrompt()
    }

    @Test
    fun `given refresh, when not invoked as PTR, then pull-to-refresh request is not tracked`() {
        initSelectedSite()

        viewModel.refresh()

        verify(analyticsTrackerWrapper, times(0)).track(Stat.MY_SITE_PULL_TO_REFRESH)
    }

    @Test
    fun `given jp app, when onResume invoked, then dashboard cards are fetched`() {
        initSelectedSite(isJetpackApp = true)

        viewModel.onResume()

        verify(dashboardCardsViewModelSlice).buildCards(site)
        verify(dashboardItemsViewModelSlice).clearValue()
    }

    @Test
    fun `given wp app, when onResume invoked, then site items are fetched`() {
        initSelectedSite(isJetpackApp = false)

        viewModel.refresh()

        verify(dashboardItemsViewModelSlice).buildItems(site)
        verify(dashboardCardsViewModelSlice).clearValue()
    }



    @Test
    fun `given jp app, when refresh invoked, then dashboard cards are refreshed`() {
        initSelectedSite(isJetpackApp = true)

        viewModel.refresh()

        verify(dashboardCardsViewModelSlice).buildCards(site)
        verify(dashboardItemsViewModelSlice).clearValue()
    }

    @Test
    fun `given wp app, when refresh invoked, then site items are refreshed`() {
        initSelectedSite(isJetpackApp = false)

        viewModel.refresh()

        verify(dashboardItemsViewModelSlice).buildItems(site)
        verify(dashboardCardsViewModelSlice).clearValue()
    }



    /* LAND ON THE EDITOR A/B EXPERIMENT */
    @Test
    fun `given the land on the editor feature is enabled, then the home page editor is shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.performFirstStepAfterSiteCreation(isSiteTitleTaskCompleted = false, isNewSite = true)

        verify(analyticsTrackerWrapper).track(Stat.LANDING_EDITOR_SHOWN)
        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.OpenHomepage(site, homepageLocalId = localHomepageId, isNewSite = true)
        )
    }

    @Test
    fun `given the land on the editor feature is not enabled, then the home page editor is not shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.performFirstStepAfterSiteCreation(isSiteTitleTaskCompleted = false, isNewSite = true)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `when onActionableEmptyViewVisible is invoked then show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(true)

            viewModel.onActionableEmptyViewVisible()
            advanceUntilIdle()

            assertThat(viewModel.onShowJetpackIndividualPluginOverlay.value?.peekContent()).isEqualTo(Unit)
        }

    @Test
    fun `when onActionableEmptyViewVisible is invoked then don't show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(false)

            viewModel.onActionableEmptyViewVisible()
            advanceUntilIdle()

            assertThat(viewModel.onShowJetpackIndividualPluginOverlay.value?.peekContent()).isNull()
        }


    @Test
    fun `when onCleared is called, then clears all the vm slices`() {
        viewModel.invokeOnCleared()

        verify(siteInfoHeaderCardViewModelSlice).onCleared()
        verify(accountDataViewModelSlice).onCleared()
        verify(dashboardCardsViewModelSlice).onCleared()
        verify(dashboardItemsViewModelSlice).onCleared()
    }

    @Suppress("LongParameterList")
    private fun initSelectedSite(
        isQuickStartInProgress: Boolean = false,
        isSiteUsingWpComRestApi: Boolean = true,
        isJetpackApp: Boolean = false
    ) {
        quickStartUpdate.value = QuickStartUpdate(
            categories = if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList()
        )
        // in order to build the dashboard cards, this value should be true along with isSiteUsingWpComRestApi
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(isJetpackApp)

        if (isSiteUsingWpComRestApi) {
            site.setIsWPCom(true)
            site.setIsJetpackConnected(true)
            site.origin = SiteModel.ORIGIN_WPCOM_REST
        }
        onSiteSelected.value = siteLocalId
        onSiteChange.value = site
        selectedSite.value = SelectedSite(site)
    }

    fun ViewModel.invokeOnCleared() {
        val viewModelStore = ViewModelStore()
        val viewModelProvider = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = this@invokeOnCleared as T
        })
        viewModelProvider[this@invokeOnCleared::class.java]
        viewModelStore.clear()
    }
}
