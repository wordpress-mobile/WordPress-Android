package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar.Callback
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartSiteMenuStep
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

class QuickStartRepositoryTest : BaseUnitTest() {
    @Mock lateinit var quickStartStore: QuickStartStore
    @Mock lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var eventBus: EventBusWrapper
    @Mock lateinit var dynamicCardStore: DynamicCardStore
    @Mock lateinit var htmlCompat: HtmlCompatWrapper
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var quickStartSiteMenuStep: MutableList<QuickStartSiteMenuStep?>
    private val siteLocalId = 1

    private val siteMenuTasks = listOf(
            QuickStartTask.VIEW_SITE,
            QuickStartTask.ENABLE_POST_SHARING,
            QuickStartTask.EXPLORE_PLANS,
            QuickStartTask.CHECK_STATS,
            QuickStartTask.REVIEW_PAGES,
            QuickStartTask.EDIT_HOMEPAGE
    )

    private val nonSiteMenuTasks = QuickStartTask.values().subtract(siteMenuTasks)

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        quickStartRepository = QuickStartRepository(
                TEST_DISPATCHER,
                quickStartStore,
                quickStartUtilsWrapper,
                appPrefsWrapper,
                selectedSiteRepository,
                resourceProvider,
                analyticsTrackerWrapper,
                dispatcher,
                eventBus,
                dynamicCardStore,
                htmlCompat,
                quickStartDynamicCardsFeatureConfig,
                contextProvider,
                htmlMessageUtils,
                buildConfigWrapper,
                mySiteDashboardTabsFeatureConfig
        )
        snackbars = mutableListOf()
        quickStartPrompts = mutableListOf()
        quickStartSiteMenuStep = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        quickStartRepository.onQuickStartMySitePrompts.observeForever { event ->
            event?.getContentIfNotHandled()?.let { quickStartPrompts.add(it) }
        }
        quickStartRepository.onQuickStartSiteMenuStep.observeForever {
            quickStartSiteMenuStep.add(it)
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

        QuickStartTask.values().forEach { verify(quickStartStore).setDoneTask(siteLocalId.toLong(), it, true) }
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

        quickStartRepository.setActiveTask(QuickStartTask.PUBLISH_POST)

        reset(quickStartStore)

        quickStartRepository.completeTask(QuickStartTask.UPDATE_SITE_TITLE)

        verifyZeroInteractions(quickStartStore)
    }

    /* QUICK START REQUEST SITE MENU STEP */

    @Test
    fun `given task origin site menu tab, when site menu task is activated, then site menu step is not started`() {
        quickStartRepository.quickStartTaskOrigin = MySiteTabType.SITE_MENU
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(siteMenuTasks.random())

        assertThat(quickStartSiteMenuStep).isEmpty()
    }

    @Test
    fun `given task origin dashboard tab, when site menu task is activated, then site menu step is started`() {
        quickStartRepository.quickStartTaskOrigin = MySiteTabType.DASHBOARD
        initQuickStartInProgress()
        val task = siteMenuTasks.random()

        quickStartRepository.setActiveTask(task)

        assertThat(quickStartSiteMenuStep.last()).isEqualTo(QuickStartSiteMenuStep(true, task))
    }

    @Test
    fun `given task origin dashboard tab, when non site menu task is activated, then site menu step is not started`() {
        quickStartRepository.quickStartTaskOrigin = MySiteTabType.DASHBOARD
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(nonSiteMenuTasks.random())

        assertThat(quickStartSiteMenuStep).isEmpty()
    }

    @Test
    fun `given task origin dashboard tab, when site menu task is activated, then snackbar is shown`() {
        quickStartRepository.quickStartTaskOrigin = MySiteTabType.DASHBOARD
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(siteMenuTasks.random())

        assertThat(snackbars).isNotEmpty
    }

    /* QUICK START REQUEST NEXT STEP */

    @Test
    fun `requestNextStepOfTask emits quick start event`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(QuickStartTask.ENABLE_POST_SHARING)
        quickStartRepository.requestNextStepOfTask(QuickStartTask.ENABLE_POST_SHARING)

        verify(eventBus).postSticky(QuickStartEvent(QuickStartTask.ENABLE_POST_SHARING))
    }

    /* QUICK START REMINDER NOTIFICATION */

    @Test
    fun `given active task != completed task, when task is completed, then reminder notifs are not triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.setActiveTask(QuickStartTask.PUBLISH_POST)

        quickStartRepository.completeTask(QuickStartTask.UPDATE_SITE_TITLE)

        verify(quickStartUtilsWrapper, never()).completeTaskAndRemindNextOne(any(), any(), any(), any())
    }

    @Test
    fun `given active task = completed task, when task is completed, then reminder notifs are triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.setActiveTask(QuickStartTask.PUBLISH_POST)

        quickStartRepository.completeTask(QuickStartTask.PUBLISH_POST)

        verify(quickStartUtilsWrapper).completeTaskAndRemindNextOne(
                QuickStartTask.PUBLISH_POST,
                site,
                QuickStartEvent(QuickStartTask.PUBLISH_POST),
                contextProvider.getContext()
        )
    }

    /* QUICK START NOTICE */

    @Test
    fun `given uncompleted task exists, when show quick start notice is triggered, then snackbar is shown`() = test {
        initStore(nextUncompletedTask = QuickStartTask.PUBLISH_POST)

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
        initStore(nextUncompletedTask = QuickStartTask.PUBLISH_POST)
        quickStartRepository.checkAndShowQuickStartNotice()

        snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_SWIPE)

        verify(appPrefsWrapper).setLastSkippedQuickStartTask(QuickStartTask.PUBLISH_POST)
    }

    @Test
    fun `when show quick start notice dismissed using non swipe-to-dismiss action, then the task is not skipped`() =
            test {
                initStore(nextUncompletedTask = QuickStartTask.PUBLISH_POST)
                quickStartRepository.checkAndShowQuickStartNotice()

                snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_ACTION)

                verify(appPrefsWrapper, never()).setLastSkippedQuickStartTask(QuickStartTask.PUBLISH_POST)
            }

    private fun initQuickStartInProgress() {
        initStore()
    }

    private fun initStore(
        nextUncompletedTask: QuickStartTask? = null
    ) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartInProgress(siteLocalId)).thenReturn(true)
        whenever(appPrefsWrapper.isQuickStartNoticeRequired()).thenReturn(true)
        whenever(quickStartUtilsWrapper.getNextUncompletedQuickStartTask(siteLocalId.toLong()))
                .thenReturn(nextUncompletedTask)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormat(anyOrNull())).thenReturn("")
        whenever(resourceProvider.getString(anyOrNull(), anyOrNull())).thenReturn("")
        whenever(htmlCompat.fromHtml(anyOrNull(), eq(HtmlCompat.FROM_HTML_MODE_COMPACT))).thenReturn("")
    }
}
