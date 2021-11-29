package org.wordpress.android.viewmodel.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.test
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel.FocusPointInfo

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
    private lateinit var activeTask: MutableLiveData<QuickStartTask?>
    private lateinit var externalFocusPointEvents: MutableList<List<FocusPointInfo>>
    private var fabUiState: MainFabUiState? = null

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(false)
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(850)
        whenever(buildConfigWrapper.getAppVersionName()).thenReturn("14.7")
        activeTask = MutableLiveData()
        externalFocusPointEvents = mutableListOf()
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        viewModel = WPMainActivityViewModel(
                featureAnnouncementProvider,
                buildConfigWrapper,
                appPrefsWrapper,
                analyticsTrackerWrapper,
                quickStartRepository,
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

        loginFlowTriggered = false
        switchTabTriggered = false
    }

    /* FAB VISIBILITY */

    @Test
    fun `given wordpress app, when page changed to my site, then fab is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given wordpress app, when page changed away from my site, then fab is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given wordpress app, when my site page is resumed, then fab is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isTrue
    }

    @Test
    fun `given wordpress app, when non my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given jetpack app, when page changed to my site, then fab is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given jetpack app, when page changed away from my site, then fab is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given jetpack app, when my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    @Test
    fun `given jetpack app, when non my site page is resumed, then fab is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabVisible).isFalse
    }

    /* FAB TOOLTIP VISIBILITY */

    @Test
    fun `given wordpress app, when page changed to my site, then fab tooltip is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isTrue
    }

    @Test
    fun `given wordpress app, when page changed away from my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given wordpress app, when my site page is resumed, then fab tooltip is visible`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isTrue
    }

    @Test
    fun `given wordpress app, when non my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters()

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given jetpack app, when page changed to my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given jetpack app, when page changed away from my site, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onPageChanged(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given jetpack app, when my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onResume(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    @Test
    fun `given jetpack app, when non my site page is resumed, then fab tooltip is hidden`() {
        startViewModelWithDefaultParameters(isJetpackApp = true)

        viewModel.onResume(isOnMySitePageWithValidSite = false, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFabTooltipVisible).isFalse
    }

    /* FAB TOOLTIP DISABLED */

    @Test
    fun `fab tooltip disabled when tapped`() {
        startViewModelWithDefaultParameters()
        viewModel.onTooltipTapped(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `fab tooltip disabled when user without full access to content uses the fab`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(initSite(hasFullAccessToContent = false))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `fab tooltip disabled when bottom sheet opened`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `fab tooltip disabled when fab long pressed`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabLongPressed(initSite(hasFullAccessToContent = true))
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(fabUiState?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `fab focus point visible when active task is PUBLISH_POST`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isTrue()
    }

    @Test
    fun `fab focus point gone when active task is different`() {
        startViewModelWithDefaultParameters()
        activeTask.value = UPDATE_SITE_TITLE
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isFalse()
    }

    @Test
    fun `fab focus point gone when active task is null`() {
        startViewModelWithDefaultParameters()
        activeTask.value = null
        viewModel.onPageChanged(isOnMySitePageWithValidSite = true, site = initSite(hasFullAccessToContent = true))

        assertThat(fabUiState?.isFocusPointVisible).isFalse()
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
    fun `bottom sheet does not show quick start focus point by default`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue()
        assertThat(viewModel.mainActions.value?.any { it is CreateAction && it.showQuickStartFocusPoint }).isEqualTo(
                false
        )
    }

    @Test
    fun `CREATE_NEW_POST action in bottom sheet with active Quick Start completes task and hides the focus point`() {
        startViewModelWithDefaultParameters()
        activeTask.value = PUBLISH_POST
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue()
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
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue()
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
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = false, supportsStories = false))
        assertThat(viewModel.isBottomSheetShowing.value).isNull()
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content and has all 3 options`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = true))
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(4) // 3 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue()
    }

    @Test
    fun `bottom sheet is visualized when user has partial access and has only 2 options`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(site = initSite(hasFullAccessToContent = false))
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(3) // 2 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isTrue()
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
    fun `given wordpress app, when app is launched, then feature announcement is shown`() = test {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
                featureAnnouncement
        )

        startViewModelWithDefaultParameters(false)
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `given jetpack app, when app is launched, then feature announcement is not shown`() = test {
        startViewModelWithDefaultParameters(true)
        resumeViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `when the active task needs to show an focus point, emit visible focus point info`() {
        activeTask.value = FOLLOW_SITE

        assertThat(externalFocusPointEvents).containsExactly(listOf(visibleFollowSiteFocusPointInfo))
    }

    @Test
    fun `when the active task doesn't need to show an external focus point, emit invisible focus point info`() {
        activeTask.value = VIEW_SITE

        assertThat(externalFocusPointEvents).containsExactly(listOf(invisibleFollowSiteFocusPointInfo))
    }

    @Test
    fun `when the active task is null, emit invisible focus point info`() {
        activeTask.value = null

        assertThat(externalFocusPointEvents).containsExactly(listOf(invisibleFollowSiteFocusPointInfo))
    }

    @Test
    fun `when the active task changes more than once, only emit focus point event if its value has changed`() {
        activeTask.value = FOLLOW_SITE
        activeTask.value = FOLLOW_SITE
        activeTask.value = VIEW_SITE
        activeTask.value = null
        activeTask.value = FOLLOW_SITE

        assertThat(externalFocusPointEvents).containsExactly(
                listOf(visibleFollowSiteFocusPointInfo),
                listOf(invisibleFollowSiteFocusPointInfo),
                listOf(visibleFollowSiteFocusPointInfo)
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

    private fun startViewModelWithDefaultParameters(isJetpackApp: Boolean = false) {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(isJetpackApp)
        viewModel.start(site = initSite(hasFullAccessToContent = true, supportsStories = true))
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

    private fun initSite(hasFullAccessToContent: Boolean = true, supportsStories: Boolean = true): SiteModel {
        return SiteModel().apply {
            hasCapabilityEditPages = hasFullAccessToContent
            setIsWPCom(supportsStories)
        }
    }

    companion object {
        val visibleFollowSiteFocusPointInfo = FocusPointInfo(FOLLOW_SITE, true)
        val invisibleFollowSiteFocusPointInfo = FocusPointInfo(FOLLOW_SITE, false)
    }
}
