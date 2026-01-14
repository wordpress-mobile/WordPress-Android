package org.wordpress.android.viewmodel.main

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.main.MainActionListItem.ActionType.ANSWER_BLOGGING_PROMPT
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST_FROM_AUDIO
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.main.analytics.MainCreateSheetTracker
import org.wordpress.android.ui.main.utils.MainCreateSheetHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.privacy.banner.domain.ShouldAskPrivacyConsent
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import kotlin.test.assertNotNull

@Suppress("LargeClass")
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WPMainActivityViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WPMainActivityViewModel

    private var switchTabTriggered: Boolean = false

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider

    @Mock
    lateinit var onFeatureAnnouncementRequestedObserver: Observer<Unit?>

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var bloggingPromptsStore: BloggingPromptsStore

    @Mock
    private lateinit var openBloggingPromptsOnboardingObserver: Observer<Unit?>

    @Mock
    private lateinit var shouldAskPrivacyConsent: ShouldAskPrivacyConsent

    @Mock
    private lateinit var mainCreateSheetHelper: MainCreateSheetHelper

    @Mock
    private lateinit var mainCreateSheetTracker: MainCreateSheetTracker

    private val featureAnnouncement = FeatureAnnouncement(
        "14.7",
        2,
        "14.5",
        "14.7",
        emptyList(),
        "https://wordpress.org/",
        true,
        listOf(
            FeatureAnnouncementItem(
                "Test Feature 1",
                "Test Description 1",
                "",
                "https://wordpress.org/icon1.png"
            )
        )
    )

    private val bloggingPrompt = BloggingPromptsResult(
        model = BloggingPromptModel(
            id = 123,
            text = "title",
            date = Date(),
            isAnswered = false,
            attribution = "",
            respondentsCount = 5,
            respondentsAvatarUrls = listOf(),
            answeredLink = "https://wordpress.com/tag/dailyprompt-123",
        )
    )
    private var fabUiState: MainFabUiState? = null

    @Before
    fun setUp() = runBlocking {
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(850)
        whenever(buildConfigWrapper.getAppVersionName()).thenReturn("14.7")
        whenever(bloggingPromptsStore.getPromptForDate(any(), any())).thenReturn(flowOf(bloggingPrompt))
        whenever(shouldAskPrivacyConsent()).thenReturn(false)
        whenever(mainCreateSheetHelper.canCreatePost()).thenReturn(true)
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(false)
        viewModel = WPMainActivityViewModel(
            featureAnnouncementProvider,
            buildConfigWrapper,
            appPrefsWrapper,
            analyticsTrackerWrapper,
            selectedSiteRepository,
            accountStore,
            siteStore,
            bloggingPromptsStore,
            shouldAskPrivacyConsent,
            mainCreateSheetHelper,
            mainCreateSheetTracker,
            NoDelayCoroutineDispatcher(),
        )
        viewModel.onFeatureAnnouncementRequested.observeForever(
            onFeatureAnnouncementRequestedObserver
        )
        // mainActions is MediatorLiveData and needs observer in order for us to access it's value
        viewModel.mainActions.observeForever { }
        viewModel.fabUiState.observeForever { fabUiState = it }
        viewModel.openBloggingPromptsOnboarding.observeForever(openBloggingPromptsOnboardingObserver)

        switchTabTriggered = false
    }

    /* FAB VISIBILITY */

    @Test
    fun `given fab enabled and page changed to supported page, then fab is visible`() {
        startViewModelWithDefaultParameters()
        whenever(mainCreateSheetHelper.shouldShowFabForPage(any())).thenReturn(true)

        viewModel.onPageChanged(site = initSite(hasFullAccessToContent = true), hasValidSite = true, page = mock())

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given fab disabled or page changed to non-supported page, then fab is hidden`() {
        startViewModelWithDefaultParameters()
        whenever(mainCreateSheetHelper.shouldShowFabForPage(any())).thenReturn(false)

        viewModel.onPageChanged(site = initSite(hasFullAccessToContent = true), hasValidSite = true, page = mock())

        assertThat(fabUiState?.isFabVisible).isFalse()
    }

    @Test
    fun `given fab enabled and supported page is resumed, then fab is visible`() {
        startViewModelWithDefaultParameters()
        whenever(mainCreateSheetHelper.shouldShowFabForPage(any())).thenReturn(true)

        viewModel.onResume(site = initSite(hasFullAccessToContent = true), hasValidSite = true, page = mock())

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given fab disabled or non-supported page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters()
        whenever(mainCreateSheetHelper.shouldShowFabForPage(any())).thenReturn(false)

        viewModel.onResume(site = initSite(hasFullAccessToContent = true), hasValidSite = true, page = mock())

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `bottom sheet action is new post when new post is tapped`() {
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_POST)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet action is new page when new page is tapped`() {
        whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_PAGE)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_PAGE)
    }

    @Test
    fun `bottom sheet does not show prompt card when prompts feature is not active`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(false)
        startViewModelWithDefaultParameters()
        val hasBloggingPromptAction = viewModel.mainActions.value?.any { it.actionType == ANSWER_BLOGGING_PROMPT }
        assertThat(hasBloggingPromptAction).isFalse()
    }

    @Test
    fun `bottom sheet does show prompt card when prompts feature is active`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val hasBloggingPromptAction = viewModel.mainActions.value?.any { it.actionType == ANSWER_BLOGGING_PROMPT }
        assertThat(hasBloggingPromptAction).isTrue()
    }

    @Test
    fun `bottom sheet action is ANSWER_BLOGGING_PROMPT when the BP answer button is clicked`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.firstOrNull {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction?
        assertThat(action).isNotNull

        val promptId = 123

        action!!.onClickAction?.invoke(promptId, BloggingPromptAttribution.BLOGANUARY)
        val createPostWithBloggingPromptValue = viewModel.createPostWithBloggingPrompt.value
        assertNotNull(createPostWithBloggingPromptValue)
        assertThat(createPostWithBloggingPromptValue).isEqualTo(promptId)
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content and has 2 options`() {
        whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(false)
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true), page = PageType.MY_SITE)
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(2) // 1 option plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content and has 3 options`() {
        whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(true)
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true), page = PageType.MY_SITE)
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(3) // 2 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta main page switches to My Site`() {
        setupObservers()

        startViewModelWithDefaultParameters()

        viewModel.onOpenLoginPage()

        assertThat(switchTabTriggered).isTrue
    }

    @Test
    fun `show feature announcement when it's available and no announcement was not shown before`() = test {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
            featureAnnouncement
        )

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
        verify(analyticsTrackerWrapper).track(FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
    }

    @Test
    fun `show feature announcement when it's available and was not shown before`() = test {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
            featureAnnouncement
        )

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
        verify(analyticsTrackerWrapper).track(FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
    }

    @Test
    fun `don't show feature announcement when cache is empty`() = test {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
            null
        )

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
        verify(featureAnnouncementProvider).getLatestFeatureAnnouncement(false)
    }

    @Test
    fun `don't show feature announcement on fresh app install`() = test {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(0)

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
        verify(appPrefsWrapper).lastFeatureAnnouncementAppVersionCode = 850
        verify(featureAnnouncementProvider).getLatestFeatureAnnouncement(false)
    }

    @Test
    fun `don't show feature announcement when it's not available`() = test {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `don't show feature announcement when it's available but previous announcement is the same as current`() =
        test {
            whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
            whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(2)
            whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
                featureAnnouncement
            )

            startViewModelWithDefaultParameters()
            resumeViewModelWithDefaultParameters()

            verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
            verify(featureAnnouncementProvider).getLatestFeatureAnnouncement(false)
        }

    @Test
    fun `don't show feature announcement after view model starts again`() = test {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
            featureAnnouncement
        )

        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `given whats new feature enabled, when app is launched, then feature announcement is shown`() = test {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
            featureAnnouncement
        )

        startViewModelWithDefaultParameters(true)
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `given whats new feature disabled, when app is launched, then feature announcement is not shown`() = test {
        startViewModelWithDefaultParameters(isWhatsNewFeatureEnabled = false)
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `bottom sheet actions are sorted in the correct order when can create post only`() {
        startViewModelWithDefaultParameters()

        val expectedOrder = listOf(
            NO_ACTION,
            CREATE_NEW_POST,
        )

        assertThat(viewModel.mainActions.value!!.map { it.actionType }).isEqualTo(expectedOrder)
    }

    @Test
    fun `bottom sheet actions are sorted in the correct order when can create post, and page`() {
        whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(true)
        startViewModelWithDefaultParameters()

        val expectedOrder = listOf(
            NO_ACTION,
            CREATE_NEW_POST,
            CREATE_NEW_PAGE
        )

        assertThat(viewModel.mainActions.value!!.map { it.actionType }).isEqualTo(expectedOrder)
    }

    @Test
    fun `bottom sheet actions are sorted in the correct order when can create post, prompts, and page`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
        whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(true)
        startViewModelWithDefaultParameters()

        val expectedOrder = listOf(
            ANSWER_BLOGGING_PROMPT,
            NO_ACTION,
            CREATE_NEW_POST,
            CREATE_NEW_PAGE
        )

        assertThat(viewModel.mainActions.value!!.map { it.actionType }).isEqualTo(expectedOrder)
    }

    @Test
    fun `bottom sheet actions are sorted in the correct order when can create post, from audio, prompts, and page`() =
        test {
            whenever(mainCreateSheetHelper.canCreatePostFromAudio(any())).thenReturn(true)
            whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
            whenever(mainCreateSheetHelper.canCreatePage(any(), any())).thenReturn(true)
            startViewModelWithDefaultParameters()

            val expectedOrder = listOf(
                ANSWER_BLOGGING_PROMPT,
                NO_ACTION,
                CREATE_NEW_POST,
                CREATE_NEW_POST_FROM_AUDIO,
                CREATE_NEW_PAGE
            )

            assertThat(viewModel.mainActions.value!!.map { it.actionType }).isEqualTo(expectedOrder)
        }

    @Test
    fun `hasMultipleSites should be true when there are more than one site`() {
        whenever(siteStore.sitesCount).thenReturn(2)
        assertThat(viewModel.hasMultipleSites).isEqualTo(true)
    }

    @Test
    fun `hasMultipleSites should be false when there is only one site`() {
        whenever(siteStore.sitesCount).thenReturn(1)
        assertThat(viewModel.hasMultipleSites).isEqualTo(false)
    }

    @Test
    fun `hasMultipleSites should be false when there are no site`() {
        whenever(siteStore.sitesCount).thenReturn(0)
        assertThat(viewModel.hasMultipleSites).isEqualTo(false)
    }

    @Test
    fun `firstSite should return the first site available in the list of sites`() {
        val sites = mock<ArrayList<SiteModel>>()
        whenever(siteStore.sites).thenReturn(sites)
        val siteModel = mock<SiteModel>()
        whenever(siteStore.hasSite()).thenReturn(true)
        whenever(sites.get(0)).thenReturn(siteModel)

        assertThat(viewModel.firstSite).isEqualTo(siteModel)
    }

    @Test
    fun `firstSite should return null when there are no sites`() {
        whenever(siteStore.hasSite()).thenReturn(false)

        assertThat(viewModel.firstSite).isEqualTo(null)
    }

    @Test
    fun `isSignedInWPComOrHasWPOrgSite should be true when user has WPCom access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        // No need to stub site methods - short-circuit evaluation means they won't be called

        assertThat(viewModel.isSignedInWPComOrHasWPOrgSite).isTrue()
    }

    @Test
    fun `isSignedInWPComOrHasWPOrgSite should be true when user has XMLRPC site`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(true)
        // No need to stub hasSiteAccessedViaWPAPI - short-circuit evaluation

        assertThat(viewModel.isSignedInWPComOrHasWPOrgSite).isTrue()
    }

    @Test
    fun `isSignedInWPComOrHasWPOrgSite should be true when user has WPAPI site`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(false)
        whenever(siteStore.hasSiteAccessedViaWPAPI()).thenReturn(true)

        assertThat(viewModel.isSignedInWPComOrHasWPOrgSite).isTrue()
    }

    @Test
    fun `isSignedInWPComOrHasWPOrgSite should be false when user has no account and no sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(false)
        whenever(siteStore.hasSiteAccessedViaWPAPI()).thenReturn(false)

        assertThat(viewModel.isSignedInWPComOrHasWPOrgSite).isFalse()
    }

    @Test
    fun `Should track analytics event when onHelpPromptActionClicked is called`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction
        action.onHelpAction?.invoke()
        verify(mainCreateSheetTracker).trackHelpPromptActionTapped(any())
    }

    @Test
    fun `Should trigger openBloggingPromptsOnboarding when onHelpPromptActionClicked is called`() = test {
        whenever(mainCreateSheetHelper.canCreatePromptAnswer()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction
        action.onHelpAction?.invoke()
        verify(openBloggingPromptsOnboardingObserver).onChanged(anyOrNull())
    }

    @Test
    fun `Should track card actions when onFabClicker is called`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(initSite(), page = PageType.MY_SITE)
        verify(mainCreateSheetTracker).trackCreateActionsSheetCard(any())
    }

    @Test
    fun `it asks for privacy consent at the start when it should`() = test {
        // Given
        whenever(shouldAskPrivacyConsent()).thenReturn(true)
        val observer: Observer<Unit> = mock()
        viewModel.askForPrivacyConsent.observeForever(observer)

        // When
        startViewModelWithDefaultParameters()

        // Then
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun `it asks for privacy consent only once, even when viewmodel is started more than once`() = test {
        // Given
        whenever(shouldAskPrivacyConsent()).thenReturn(true)
        val observer: Observer<Unit> = mock()
        viewModel.askForPrivacyConsent.observeForever(observer)

        // When
        startViewModelWithDefaultParameters()
        startViewModelWithDefaultParameters()

        // Then
        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `requests my site dashboard refresh when requestMySiteDashboardRefresh is called`() {
        startViewModelWithDefaultParameters()

        var observerCalledCount = 0
        viewModel.mySiteDashboardRefreshRequested.observeForever {
            observerCalledCount++
        }

        viewModel.requestMySiteDashboardRefresh()

        assertThat(observerCalledCount).isEqualTo(1)
    }

    private fun startViewModelWithDefaultParameters(
        isWhatsNewFeatureEnabled: Boolean = true,
        isWpcomOrJpSite: Boolean = true,
        pageType: PageType = PageType.MY_SITE,
    ) {
        whenever(buildConfigWrapper.isWhatsNewFeatureEnabled).thenReturn(isWhatsNewFeatureEnabled)
        viewModel.start(
            site = initSite(hasFullAccessToContent = true, isWpcomOrJpSite = isWpcomOrJpSite),
            page = pageType
        )
    }

    private fun setupObservers() {
        viewModel.switchToMeTab.observeForever { event ->
            event.applyIfNotHandled {
                switchTabTriggered = true
            }
        }
    }

    private fun resumeViewModelWithDefaultParameters() {
        viewModel.onResume(
            site = initSite(hasFullAccessToContent = true),
            hasValidSite = true,
            page = PageType.MY_SITE
        )
    }

    private fun initSite(
        hasFullAccessToContent: Boolean = true,
        isWpcomOrJpSite: Boolean = true
    ): SiteModel {
        return SiteModel().apply {
            hasCapabilityEditPages = hasFullAccessToContent
            setIsWPCom(isWpcomOrJpSite)
            setIsJetpackConnected(isWpcomOrJpSite)
        }
    }
}
