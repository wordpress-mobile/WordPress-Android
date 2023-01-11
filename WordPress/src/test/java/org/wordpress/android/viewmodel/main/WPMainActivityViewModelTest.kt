package org.wordpress.android.viewmodel.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask.CHECK_NOTIFICATIONS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.main.MainActionListItem.ActionType.ANSWER_BLOGGING_PROMPT
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel.FocusPointInfo
import java.util.Date

@Suppress("LargeClass")
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WPMainActivityViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WPMainActivityViewModel

    private var loginFlowTriggered: Boolean = false
    private var switchTabTriggered: Boolean = false

    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider
    @Mock lateinit var onFeatureAnnouncementRequestedObserver: Observer<Unit>
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var quickStartRepository: QuickStartRepository
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
    @Mock lateinit var bloggingPromptsStore: BloggingPromptsStore
    @Mock lateinit var quickStartType: QuickStartType
    @Mock private lateinit var openBloggingPromptsOnboardingObserver: Observer<Unit>

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
                    title = "",
                    content = "content",
                    date = Date(),
                    isAnswered = false,
                    attribution = "",
                    respondentsCount = 5,
                    respondentsAvatarUrls = listOf()
            )
    )
    private lateinit var activeTask: MutableLiveData<QuickStartTask?>
    private lateinit var externalFocusPointEvents: MutableList<List<FocusPointInfo>>
    private var fabUiState: MainFabUiState? = null

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(false)
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(850)
        whenever(buildConfigWrapper.getAppVersionName()).thenReturn("14.7")
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL))
                .thenReturn(FOLLOW_SITE)
        activeTask = MutableLiveData()
        externalFocusPointEvents = mutableListOf()
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(bloggingPromptsStore.getPromptForDate(any(), any())).thenReturn(flowOf(bloggingPrompt))
        viewModel = WPMainActivityViewModel(
                featureAnnouncementProvider,
                buildConfigWrapper,
                appPrefsWrapper,
                analyticsTrackerWrapper,
                quickStartRepository,
                selectedSiteRepository,
                accountStore,
                siteStore,
                bloggingPromptsFeatureConfig,
                bloggingPromptsStore,
                NoDelayCoroutineDispatcher()
        )
        viewModel.onFeatureAnnouncementRequested.observeForever(
                onFeatureAnnouncementRequestedObserver
        )
        viewModel.onFocusPointVisibilityChange.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                externalFocusPointEvents.add(it)
            }
        }
        // mainActions is MediatorLiveData and needs observer in order for us to access it's value
        viewModel.mainActions.observeForever { }
        viewModel.fabUiState.observeForever { fabUiState = it }
        viewModel.openBloggingPromptsOnboarding.observeForever(openBloggingPromptsOnboardingObserver)

        loginFlowTriggered = false
        switchTabTriggered = false
    }

    /* FAB VISIBILITY */

    @Test
    fun `given fab enabled, when page changed to my site, then fab is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given fab enabled, when page changed away from my site, then fab is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given fab enabled, when my site page is resumed, then fab is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given fab enabled, when non my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given fab disabled, when page changed to my site, then fab is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given fab disabled, when page changed away from my site, then fab is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given fab disabled, when my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given fab disabled, when non my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    /* FAB TOOLTIP VISIBILITY */

    @Test
    fun `given fab enabled, when page changed to my site, then fab tooltip is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isTrue
    }

    @Test
    fun `given fab enabled, when page changed away from my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given fab enabled, when my site page is resumed, then fab tooltip is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isTrue
    }

    @Test
    fun `given fab enabled, when non my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given fab disabled, when page changed to my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given fab disabled, when page changed away from my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given fab disabled, when my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given fab disabled, when non my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isCreateFabEnabled = false)

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    /* FAB TOOLTIP DISABLED */

    @Test
    fun `fab tooltip disabled when tapped`() {
        startViewModelWithDefaultParameters()
        viewModel.onTooltipTapped(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `fab tooltip disabled when user without full access to content uses the fab`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(initSite(hasFullAccessToContent = false))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `fab tooltip disabled when bottom sheet opened`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `fab tooltip disabled when fab long pressed`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabLongPressed(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `fab focus point visible when active task is PUBLISH_POST`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isTrue
    }

    @Test
    fun `fab focus point gone when active task is different`() {
        startViewModelWithDefaultParameters()
        activeTask.value = UPDATE_SITE_TITLE
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isFalse
    }

    @Test
    fun `fab focus point gone when active task is null`() {
        startViewModelWithDefaultParameters()
        activeTask.value = null
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isFalse
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
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_PAGE)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_PAGE)
    }

    @Test
    fun `bottom sheet action is new story when new story is tapped`() {
        viewModel.start(site = initSite(hasFullAccessToContent = true))
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_STORY } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_STORY)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_STORY)
    }

    @Test
    fun `bottom sheet does not show prompt card when FF is OFF`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        startViewModelWithDefaultParameters()
        val hasBloggingPromptAction = viewModel.mainActions.value?.any { it.actionType == ANSWER_BLOGGING_PROMPT }
        assertThat(hasBloggingPromptAction).isFalse()
    }

    @Test
    fun `bottom sheet does show prompt card when FF is ON`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val hasBloggingPromptAction = viewModel.mainActions.value?.any { it.actionType == ANSWER_BLOGGING_PROMPT }
        assertThat(hasBloggingPromptAction).isTrue()
    }

    @Test
    fun `bottom sheet does not show prompt card when site is self-hosted`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters(isWpcomOrJpSite = false)
        val hasBloggingPromptAction = viewModel.mainActions.value?.any { it.actionType == ANSWER_BLOGGING_PROMPT }
        assertThat(hasBloggingPromptAction).isFalse()
    }

    @Test
    fun `bottom sheet action is ANSWER_BLOGGING_PROMPT when the BP answer button is clicked`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.firstOrNull {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction?
        assertThat(action).isNotNull

        val promptId = 123

        action!!.onClickAction?.invoke(promptId)
        assertThat(viewModel.createPostWithBloggingPrompt.value).isEqualTo(promptId)
    }

    @Test
    fun `bottom sheet does not show quick start focus point by default`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                false
        )
    }

    @Test
    fun `CREATE_NEW_POST action in bottom sheet with active Quick Start completes task and hides the focus point`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                true
        )

        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_POST)
        verify(quickStartRepository).completeTask(any())

        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                false
        )
    }

    @Test
    fun `CREATE_NEW_POST action sets task as done in QuickStartRepository`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))

        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_POST)

        verify(quickStartRepository).completeTask(PUBLISH_POST)
    }

    @Test
    fun `actions that are not CREATE_NEW_POST will not complete quick start task`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                true
        )

        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_PAGE)

        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                false
        )
    }

    @Test
    fun `new post action is triggered from FAB when no full access to content if stories unavailable`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = false, isWpcomOrJpSite = false))
        assertThat(viewModel.isBottomSheetShowing.value).isNull()
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content and has all 3 options`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(4) // 3 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
    }

    @Test
    fun `bottom sheet is visualized when user has partial access and has only 2 options`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = false))
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(3) // 2 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta the login page flow is started`() {
        setupObservers()

        startViewModelWithDefaultParameters()

        viewModel.onOpenLoginPage(0)

        assertThat(loginFlowTriggered).isTrue
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta default page is set to My Site`() {
        setupObservers()

        startViewModelWithDefaultParameters()

        viewModel.onOpenLoginPage(0)

        verify(appPrefsWrapper, times(1)).setMainPageIndex(eq(0))
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta main page switches to My Site`() {
        setupObservers()

        startViewModelWithDefaultParameters()

        viewModel.onOpenLoginPage(0)

        assertThat(switchTabTriggered).isTrue
    }

    @Test
    fun `onResume set expected content message when user has full access to content`() {
        startViewModelWithDefaultParameters()
        resumeViewModelWithDefaultParameters()
        assertThat(fabUiState!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_page_fab_tooltip_stories_enabled)
    }

    @Test
    fun `onResume set expected content message when user has not full access to content`() {
        startViewModelWithDefaultParameters()
        viewModel.onResume(site = initSite(hasFullAccessToContent = false), isOnMySitePageWithValidSite = true)
        assertThat(fabUiState!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_page_fab_tooltip_contributors_stories_enabled)
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
    fun `when follow site is active task, then only follow site visible focus point shown`() {
        activeTask.value = FOLLOW_SITE

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(visibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo)
        )
    }

    @Test
    fun `when check notifications is active task, then only check notifications visible focus point shown`() {
        activeTask.value = CHECK_NOTIFICATIONS

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(invisibleFollowSiteFocusPointInfo, visibleCheckNotificationsFocusPointInfo)
        )
    }

    @Test
    fun `when the active task doesn't need to show an external focus point, emit invisible focus point info`() {
        activeTask.value = VIEW_SITE

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(invisibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo)
        )
    }

    @Test
    fun `when the active task is null, emit invisible focus point info`() {
        activeTask.value = null

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(invisibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo)
        )
    }

    @Test
    fun `when the active task changes more than once, only emit focus point event if its value has changed`() {
        activeTask.value = FOLLOW_SITE
        activeTask.value = FOLLOW_SITE
        activeTask.value = VIEW_SITE
        activeTask.value = null
        activeTask.value = FOLLOW_SITE

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(visibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo),
                listOf(invisibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo),
                listOf(visibleFollowSiteFocusPointInfo, invisibleCheckNotificationsFocusPointInfo)
        )
    }

    @Test
    fun `bottom sheet actions are sorted in the correct order`() {
        startViewModelWithDefaultParameters()

        val expectedOrder = listOf(
                NO_ACTION,
                CREATE_NEW_STORY,
                CREATE_NEW_POST,
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
    fun `Should track analytics event when onHelpPrompActionClicked is called`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction
        action.onHelpAction?.invoke()
        verify(analyticsTrackerWrapper).track(Stat.MY_SITE_CREATE_SHEET_PROMPT_HELP_TAPPED)
    }

    @Test
    fun `Should trigger openBloggingPromptsOnboarding when onHelpPrompActionClicked is called`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first {
            it.actionType == ANSWER_BLOGGING_PROMPT
        } as AnswerBloggingPromptAction
        action.onHelpAction?.invoke()
        verify(openBloggingPromptsOnboardingObserver).onChanged(anyOrNull())
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Should track BLOGGING_PROMPTS_CREATE_SHEET_CARD_VIEWED when onFabClicked is called and actions contains AnswerBloggingPromptAction`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(initSite())
        verify(analyticsTrackerWrapper).track(Stat.BLOGGING_PROMPTS_CREATE_SHEET_CARD_VIEWED)
    }

    private fun startViewModelWithDefaultParameters(
        isWhatsNewFeatureEnabled: Boolean = true,
        isCreateFabEnabled: Boolean = true,
        isWpcomOrJpSite: Boolean = true
    ) {
        whenever(buildConfigWrapper.isWhatsNewFeatureEnabled).thenReturn(isWhatsNewFeatureEnabled)
        whenever(buildConfigWrapper.isCreateFabEnabled).thenReturn(isCreateFabEnabled)
        viewModel.start(site = initSite(hasFullAccessToContent = true, isWpcomOrJpSite = isWpcomOrJpSite))
    }

    private fun setupObservers() {
        viewModel.startLoginFlow.observeForever { event ->
            event.applyIfNotHandled {
                loginFlowTriggered = true
            }
        }

        viewModel.switchToMySite.observeForever { event ->
            event.applyIfNotHandled {
                switchTabTriggered = true
            }
        }
    }

    private fun resumeViewModelWithDefaultParameters() {
        viewModel.onResume(site = initSite(hasFullAccessToContent = true), isOnMySitePageWithValidSite = true)
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

    companion object {
        val visibleFollowSiteFocusPointInfo = FocusPointInfo(FOLLOW_SITE, true)
        val invisibleFollowSiteFocusPointInfo = FocusPointInfo(FOLLOW_SITE, false)

        val visibleCheckNotificationsFocusPointInfo = FocusPointInfo(CHECK_NOTIFICATIONS, true)
        val invisibleCheckNotificationsFocusPointInfo = FocusPointInfo(CHECK_NOTIFICATIONS, false)
    }
}
