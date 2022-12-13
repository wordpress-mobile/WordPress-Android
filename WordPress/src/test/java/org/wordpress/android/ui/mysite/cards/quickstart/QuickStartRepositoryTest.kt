package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar.Callback
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartTabStep
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.QuickStartExistingUsersV2FeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

class QuickStartRepositoryTest : BaseUnitTest() {
    @Mock lateinit var quickStartStore: QuickStartStore
    @Mock lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var eventBus: EventBusWrapper
    @Mock lateinit var dynamicCardStore: DynamicCardStore
    @Mock lateinit var htmlCompat: HtmlCompatWrapper
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig
    @Mock lateinit var quickStartExistingUsersV2FeatureConfig: QuickStartExistingUsersV2FeatureConfig
    @Mock lateinit var quickStartType: QuickStartType
    @Mock lateinit var quickStartTracker: QuickStartTracker
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var quickStartTabStep: MutableList<QuickStartTabStep?>
    private val siteLocalId = 1

    private val siteMenuTasks = listOf(
            QuickStartNewSiteTask.ENABLE_POST_SHARING
    )

    private val dashboardTasks = listOf(
            QuickStartNewSiteTask.CHECK_STATS,
            QuickStartNewSiteTask.REVIEW_PAGES
    )

    private val nonSiteMenuTasks = QuickStartTask.getAllTasks().subtract(siteMenuTasks)

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        whenever(appPrefsWrapper.getLastSelectedQuickStartTypeForSite(any())).thenReturn(quickStartType)
        whenever(quickStartExistingUsersV2FeatureConfig.isEnabled()).thenReturn(false)
        quickStartRepository = QuickStartRepository(
                TEST_DISPATCHER,
                quickStartStore,
                quickStartUtilsWrapper,
                appPrefsWrapper,
                selectedSiteRepository,
                resourceProvider,
                dispatcher,
                eventBus,
                dynamicCardStore,
                htmlCompat,
                quickStartDynamicCardsFeatureConfig,
                contextProvider,
                htmlMessageUtils,
                quickStartTracker,
                buildConfigWrapper,
                mySiteDashboardTabsFeatureConfig,
                quickStartExistingUsersV2FeatureConfig
        )
        snackbars = mutableListOf()
        quickStartPrompts = mutableListOf()
        quickStartTabStep = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        quickStartRepository.onQuickStartMySitePrompts.observeForever { event ->
            event?.getContentIfNotHandled()?.let { quickStartPrompts.add(it) }
        }
        quickStartRepository.onQuickStartTabStep.observeForever {
            quickStartTabStep.add(it)
        }
        site = SiteModel()
        site.id = siteLocalId
    }

    /* QUICK START SKIP */

    @Test
    fun `when quick start is skipped, then all quick start tasks for the selected site are set to done`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.skipQuickStart()

        QuickStartTask.getAllTasks().forEach { verify(quickStartStore).setDoneTask(siteLocalId.toLong(), it, true) }
    }

    @Test
    fun `when quick start is skipped, then quick start is marked complete for the selected site`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.skipQuickStart()

        verify(quickStartStore).setQuickStartCompleted(siteLocalId.toLong(), true)
    }

    @Test
    fun `when quick start is skipped, then quick start notifications for the selected site are marked received`() =
            test {
                whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
                initStore()

                quickStartRepository.skipQuickStart()

                verify(quickStartStore).setQuickStartNotificationReceived(siteLocalId.toLong(), true)
            }

    /* QUICK START COMPLETE TASK */

    @Test
    fun `completeTask does not marks active task as done if it is different`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.setActiveTask(QuickStartNewSiteTask.PUBLISH_POST)

        reset(quickStartStore)

        quickStartRepository.completeTask(QuickStartNewSiteTask.UPDATE_SITE_TITLE)

        verifyNoInteractions(quickStartStore)
    }

    /* QUICK START REQUEST TAB STEP - SITE MENU */

    @Test
    fun `given task origin site menu tab, when site menu task is activated, then site menu tab step is not started`() {
        quickStartRepository.currentTab = MySiteTabType.SITE_MENU
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(siteMenuTasks.random())

        assertThat(quickStartTabStep).isEmpty()
    }

    @Test
    fun `given task origin dashboard tab, when site menu task is activated, then site menu tab step is started`() {
        quickStartRepository.currentTab = MySiteTabType.DASHBOARD
        quickStartRepository.quickStartTaskOriginTab = MySiteTabType.SITE_MENU
        initQuickStartInProgress()
        val task = siteMenuTasks.random()

        quickStartRepository.setActiveTask(task)

        assertThat(quickStartTabStep.last()).isEqualTo(QuickStartTabStep(true, task, MySiteTabType.SITE_MENU))
    }

    @Test
    fun `given task origin dashboard tab, when site menu task is activated, then snackbar is shown`() {
        quickStartRepository.currentTab = MySiteTabType.DASHBOARD
        quickStartRepository.quickStartTaskOriginTab = MySiteTabType.SITE_MENU
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(siteMenuTasks.random())

        assertThat(snackbars).isNotEmpty
    }

    @Test
    fun `given task origin dashboard tab, when non site menu task is activated, then site menu step is not started`() {
        quickStartRepository.currentTab = MySiteTabType.DASHBOARD
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(nonSiteMenuTasks.random())

        assertThat(quickStartTabStep).isEmpty()
    }

    /* QUICK START REQUEST TAB STEP - DASHBOARD */

    @Test
    fun `given task origin + current tab is dashboard, when task is activated, then tab step is not started`() {
        quickStartRepository.currentTab = MySiteTabType.DASHBOARD
        quickStartRepository.quickStartTaskOriginTab = MySiteTabType.DASHBOARD
        val task = dashboardTasks.random()
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(task)

        assertThat(quickStartTabStep).isEmpty()
    }

    @Test
    fun `given new site + task origin dashboard + current tab menu, when task activated, then tab step started`() {
        quickStartRepository.currentTab = MySiteTabType.SITE_MENU
        quickStartRepository.quickStartTaskOriginTab = MySiteTabType.DASHBOARD
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
                .thenReturn(QuickStartNewSiteTask.CHECK_STATS)
        val task = dashboardTasks.random()
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(task)

        assertThat(quickStartTabStep.last()).isEqualTo(QuickStartTabStep(true, task, MySiteTabType.DASHBOARD))
    }

    @Test
    fun `given existing site + task origin dashboard + current tab menu, when task activated, then tab step started`() {
        quickStartRepository.currentTab = MySiteTabType.SITE_MENU
        quickStartRepository.quickStartTaskOriginTab = MySiteTabType.DASHBOARD
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
                .thenReturn(QuickStartExistingSiteTask.CHECK_STATS)
        val task = QuickStartExistingSiteTask.CHECK_STATS
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(task)

        assertThat(quickStartTabStep.last()).isEqualTo(QuickStartTabStep(true, task, MySiteTabType.DASHBOARD))
    }

    /* QUICK START REQUEST NEXT STEP */

    @Test
    fun `requestNextStepOfTask emits quick start event`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(QuickStartNewSiteTask.ENABLE_POST_SHARING)
        quickStartRepository.requestNextStepOfTask(QuickStartNewSiteTask.ENABLE_POST_SHARING)

        verify(eventBus).postSticky(QuickStartEvent(QuickStartNewSiteTask.ENABLE_POST_SHARING))
    }

    /* QUICK START REMINDER NOTIFICATION */

    @Test
    fun `given active task != completed task, when task is completed, then reminder notifs are not triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.setActiveTask(QuickStartNewSiteTask.PUBLISH_POST)

        quickStartRepository.completeTask(QuickStartNewSiteTask.UPDATE_SITE_TITLE)

        verify(quickStartUtilsWrapper, never()).completeTaskAndRemindNextOne(any(), any(), any(), any(), any())
    }

    @Test
    fun `given active task = completed task, when task is completed, then reminder notifs are triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.setActiveTask(QuickStartNewSiteTask.PUBLISH_POST)

        quickStartRepository.completeTask(QuickStartNewSiteTask.PUBLISH_POST)

        verify(quickStartUtilsWrapper).completeTaskAndRemindNextOne(
                QuickStartNewSiteTask.PUBLISH_POST,
                site,
                QuickStartEvent(QuickStartNewSiteTask.PUBLISH_POST),
                contextProvider.getContext(),
                quickStartType
        )
    }

    /* QUICK START NOTICE */

    @Test
    fun `given uncompleted task exists, when show quick start notice is triggered, then snackbar is shown`() = test {
        initStore(nextUncompletedTask = QuickStartNewSiteTask.PUBLISH_POST)

        quickStartRepository.checkAndShowQuickStartNotice()

        assertThat(snackbars).isNotEmpty
    }

    @Test
    fun `given uncompleted task not exists, when show quick start notice is triggered, then snackbar not shown`() =
            test {
                initStore(nextUncompletedTask = null)

                quickStartRepository.checkAndShowQuickStartNotice()

                assertThat(snackbars).isEmpty()
            }

    @Test
    fun `when show quick start notice dismissed using swipe-to-dismiss action, then the task is skipped`() = test {
        initStore(nextUncompletedTask = QuickStartNewSiteTask.PUBLISH_POST)
        quickStartRepository.checkAndShowQuickStartNotice()

        snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_SWIPE)

        verify(appPrefsWrapper).setLastSkippedQuickStartTask(QuickStartNewSiteTask.PUBLISH_POST)
    }

    @Test
    fun `when show quick start notice dismissed using non swipe-to-dismiss action, then the task is not skipped`() =
            test {
                initStore(nextUncompletedTask = QuickStartNewSiteTask.PUBLISH_POST)
                quickStartRepository.checkAndShowQuickStartNotice()

                snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_ACTION)

                verify(appPrefsWrapper, never()).setLastSkippedQuickStartTask(QuickStartNewSiteTask.PUBLISH_POST)
            }

    @Test
    fun `when all task are completed, then completed notice is triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore(nextUncompletedTask = QuickStartNewSiteTask.CHECK_STATS)
        quickStartRepository.setActiveTask(QuickStartNewSiteTask.CHECK_STATS)
        whenever(quickStartType.isEveryQuickStartTaskDone(quickStartStore, site.id.toLong())).thenReturn(true)

        quickStartRepository.completeTask(QuickStartNewSiteTask.CHECK_STATS)

        assertThat(snackbars).isNotEmpty
    }

    private fun initQuickStartInProgress() {
        initStore()
    }

    private fun initStore(
        nextUncompletedTask: QuickStartTask? = null
    ) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartType.isQuickStartInProgress(quickStartStore, siteLocalId.toLong())).thenReturn(true)
        whenever(appPrefsWrapper.isQuickStartNoticeRequired()).thenReturn(true)
        whenever(quickStartUtilsWrapper.getNextUncompletedQuickStartTask(quickStartType, siteLocalId.toLong()))
                .thenReturn(nextUncompletedTask)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormat(anyOrNull())).thenReturn("")
        whenever(resourceProvider.getString(anyOrNull(), anyOrNull())).thenReturn("")
        whenever(htmlCompat.fromHtml(anyOrNull(), eq(HtmlCompat.FROM_HTML_MODE_COMPACT))).thenReturn("")
    }
}
