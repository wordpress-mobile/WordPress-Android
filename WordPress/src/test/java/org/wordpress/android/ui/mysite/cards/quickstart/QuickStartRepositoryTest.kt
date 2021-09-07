package org.wordpress.android.ui.mysite.cards.quickstart

import com.google.android.material.snackbar.Snackbar.Callback
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
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
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardsModel
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.CREATE_SITE_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.PUBLISH_POST_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.SHARE_SITE_TUTORIAL
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

private const val ALL_TASKS_COMPLETED_MESSAGE = "All tasks completed!"

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
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var result: MutableList<QuickStartUpdate>
    private val siteLocalId = 1

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
                htmlMessageUtils
        )
        snackbars = mutableListOf()
        quickStartPrompts = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        quickStartRepository.onQuickStartMySitePrompts.observeForever { event ->
            event?.getContentIfNotHandled()?.let { quickStartPrompts.add(it) }
        }
        site = SiteModel()
        site.id = siteLocalId
        result = mutableListOf()
        quickStartRepository.buildSource(testScope(), siteLocalId).observeForever { result.add(it) }
    }

    @Test
    fun `refresh loads model`() = test {
        initStore()

        quickStartRepository.refresh()

        assertModel()
    }

    @Test
    fun `given dynamic card enabled + same type tasks done, when refresh started, then completion msg shown`() =
            test {
                whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(true)
                initStore()

                triggerQSRefreshAfterSameTypeTasksAreComplete()

                assertThat(snackbars).containsOnly(SnackbarMessageHolder(UiStringText(ALL_TASKS_COMPLETED_MESSAGE)))
            }

    @Test
    fun `given dynamic card disabled + same type tasks done, when refresh started, then completion msg not shown`() =
            test {
                whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
                initStore()

                triggerQSRefreshAfterSameTypeTasksAreComplete()

                assertThat(snackbars).doesNotContain(SnackbarMessageHolder(UiStringText(ALL_TASKS_COMPLETED_MESSAGE)))
            }

    @Test
    fun `given dynamic card enabled + same type tasks done, when refresh started, then dynamic card removed`() = test {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(true)
        initStore()

        triggerQSRefreshAfterSameTypeTasksAreComplete()

        verify(dynamicCardStore).removeCard(siteLocalId, GROW_QUICK_START)
    }

    @Test
    fun `given dynamic card disabled + same type tasks done, when refresh started, then dynamic card not removed`() =
            test {
                whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
                initStore()

                triggerQSRefreshAfterSameTypeTasksAreComplete()

                verify(dynamicCardStore, never()).removeCard(siteLocalId, GROW_QUICK_START)
            }

    @Test
    fun `given dynamic card disabled + same type tasks done, when refresh started, then both task types exists`() =
            test {
                whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
                initStore()

                triggerQSRefreshAfterSameTypeTasksAreComplete()

                assertThat(result.last().categories.map { it.taskType }).isEqualTo(listOf(CUSTOMIZE, GROW))
            }

    @Test
    fun `refresh does not show completion message if not all tasks of a same type have been completed`() = test {
        initStore()

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isEveryQuickStartTaskDoneForType(siteLocalId, GROW)).thenReturn(false)

        val task = PUBLISH_POST
        quickStartRepository.setActiveTask(task)
        quickStartRepository.completeTask(task)
        quickStartRepository.refresh()

        assertThat(snackbars).isEmpty()
    }

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

    @Test
    fun `start marks CREATE_SITE as done and loads model`() = test {
        initStore()

        quickStartRepository.startQuickStart(siteLocalId)

        verify(quickStartUtilsWrapper).startQuickStart(siteLocalId)
        assertModel()
    }

    @Test
    fun `sets active task and shows stylized snackbar when not UPDATE_SITE_TITLE`() = test {
        initStore()
        quickStartRepository.refresh()

        quickStartRepository.setActiveTask(PUBLISH_POST)

        assertThat(result.last().activeTask).isEqualTo(PUBLISH_POST)
        assertThat(quickStartPrompts.last()).isEqualTo(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
    }

    @Test
    fun `completeTask marks current active task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.refresh()
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)

        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteLocalId.toLong(), task, true)
        val update = result.last()
        assertThat(update.activeTask).isNull()
        assertThat(update.categories).isNotEmpty
    }

    @Test
    fun `completeTask marks current pending task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.refresh()
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)
        quickStartRepository.requestNextStepOfTask(task)
        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteLocalId.toLong(), task, true)
        val update = result.last()
        assertThat(update.activeTask).isNull()
        assertThat(update.categories).isNotEmpty
    }

    @Test
    fun `completeTask does not marks active task as done if it is different`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.setActiveTask(PUBLISH_POST)

        reset(quickStartStore)

        quickStartRepository.completeTask(UPDATE_SITE_TITLE)

        verifyZeroInteractions(quickStartStore)
    }

    @Test
    fun `requestNextStepOfTask emits quick start event`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(ENABLE_POST_SHARING)
        quickStartRepository.requestNextStepOfTask(ENABLE_POST_SHARING)

        verify(eventBus).postSticky(QuickStartEvent(ENABLE_POST_SHARING))
    }

    @Test
    fun `requestNextStepOfTask clears current active task`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(ENABLE_POST_SHARING)
        quickStartRepository.requestNextStepOfTask(ENABLE_POST_SHARING)

        val update = result.last()
        assertThat(update.activeTask).isNull()
    }

    @Test
    fun `requestNextStepOfTask does not proceed if the active task is different`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(PUBLISH_POST)
        quickStartRepository.requestNextStepOfTask(ENABLE_POST_SHARING)

        verifyZeroInteractions(eventBus)
        val update = result.last()
        assertThat(update.activeTask).isEqualTo(PUBLISH_POST)
    }

    @Test
    fun `clearActiveTask clears current active task`() = test {
        initQuickStartInProgress()

        quickStartRepository.setActiveTask(ENABLE_POST_SHARING)
        quickStartRepository.clearActiveTask()

        val update = result.last()
        assertThat(update.activeTask).isNull()
    }

    @Test
    fun `marks EDIT_HOMEPAGE task as done when site showing Posts instead of Homepage`() = test {
        initStore()

        val updatedSiteId = 2
        site.id = updatedSiteId
        site.showOnFront = ShowOnFront.POSTS.value
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartStore.hasDoneTask(updatedSiteId.toLong(), EDIT_HOMEPAGE)).thenReturn(false)

        quickStartRepository.buildSource(testScope(), updatedSiteId)

        verify(quickStartStore).setDoneTask(updatedSiteId.toLong(), EDIT_HOMEPAGE, true)
    }

    @Test
    fun `does not mark EDIT_HOMEPAGE task as done when site showing Homepage`() = test {
        val updatedSiteLocalId = 2
        site.id = updatedSiteLocalId
        site.showOnFront = ShowOnFront.PAGE.value
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        quickStartRepository.buildSource(testScope(), updatedSiteLocalId)

        verify(quickStartStore, never()).setDoneTask(updatedSiteLocalId.toLong(), EDIT_HOMEPAGE, true)
    }

    @Test
    fun `given active task != completed task, when task is completed, then reminder notifs are not triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.setActiveTask(PUBLISH_POST)

        quickStartRepository.completeTask(UPDATE_SITE_TITLE)

        verify(quickStartUtilsWrapper, never()).completeTaskAndRemindNextOne(any(), any(), any(), any())
    }

    @Test
    fun `given active task = completed task, when task is completed, then reminder notifs are triggered`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartRepository.startQuickStart(siteLocalId)
        quickStartRepository.setActiveTask(PUBLISH_POST)

        quickStartRepository.completeTask(PUBLISH_POST)

        verify(quickStartUtilsWrapper).completeTaskAndRemindNextOne(
                PUBLISH_POST,
                site,
                QuickStartEvent(PUBLISH_POST),
                contextProvider.getContext()
        )
    }

    @Test
    fun `given uncompleted task exists, when show quick start notice is triggered, then snackbar is shown`() = test {
        initStore(nextUncompletedTask = PUBLISH_POST)

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
    fun `given uncompleted task, when quick start notice button action is clicked, then the task is marked active`() =
            test {
                initStore(nextUncompletedTask = PUBLISH_POST)
                quickStartRepository.checkAndShowQuickStartNotice()

                snackbars.last().buttonAction.invoke()

                assertThat(result.last().activeTask).isEqualTo(PUBLISH_POST)
            }

    @Test
    fun `when show quick start notice dismissed using swipe-to-dismiss action, then the task is skipped`() = test {
        initStore(nextUncompletedTask = PUBLISH_POST)
        quickStartRepository.checkAndShowQuickStartNotice()

        snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_SWIPE)

        verify(appPrefsWrapper).setLastSkippedQuickStartTask(PUBLISH_POST)
    }

    @Test
    fun `when show quick start notice dismissed using non swipe-to-dismiss action, then the task is not skipped`() =
            test {
                initStore(nextUncompletedTask = PUBLISH_POST)
                quickStartRepository.checkAndShowQuickStartNotice()

                snackbars.last().onDismissAction.invoke(Callback.DISMISS_EVENT_ACTION)

                verify(appPrefsWrapper, never()).setLastSkippedQuickStartTask(PUBLISH_POST)
            }

    private fun triggerQSRefreshAfterSameTypeTasksAreComplete() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isEveryQuickStartTaskDoneForType(siteLocalId, GROW)).thenReturn(true)
        whenever(resourceProvider.getString(any())).thenReturn(ALL_TASKS_COMPLETED_MESSAGE)
        whenever(htmlCompat.fromHtml(ALL_TASKS_COMPLETED_MESSAGE)).thenReturn(ALL_TASKS_COMPLETED_MESSAGE)

        val task = PUBLISH_POST
        quickStartRepository.setActiveTask(task)
        quickStartRepository.completeTask(task)
        quickStartRepository.refresh()
    }

    private suspend fun initQuickStartInProgress() {
        initStore()
        quickStartRepository.refresh()
    }

    private suspend fun initStore(
        nextUncompletedTask: QuickStartTask? = null
    ) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(dynamicCardStore.getCards(siteLocalId)).thenReturn(
                DynamicCardsModel(
                        dynamicCardTypes = listOf(
                                CUSTOMIZE_QUICK_START,
                                GROW_QUICK_START
                        )
                )
        )
        whenever(quickStartUtilsWrapper.isQuickStartInProgress(siteLocalId)).thenReturn(true)
        whenever(appPrefsWrapper.isQuickStartNoticeRequired()).thenReturn(true)
        whenever(quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), CUSTOMIZE)).thenReturn(
                listOf(
                        CREATE_SITE
                )
        )
        whenever(quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), CUSTOMIZE)).thenReturn(
                listOf(
                        UPDATE_SITE_TITLE
                )
        )
        whenever(
                quickStartStore.getUncompletedTasksByType(
                        siteLocalId.toLong(),
                        GROW
                )
        ).thenReturn(listOf(ENABLE_POST_SHARING))
        whenever(quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), GROW)).thenReturn(listOf(PUBLISH_POST))
        whenever(quickStartUtilsWrapper.getNextUncompletedQuickStartTask(siteLocalId.toLong()))
                .thenReturn(nextUncompletedTask)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormat(anyOrNull())).thenReturn("")
    }

    private fun assertModel() {
        val quickStartUpdate = result.last()
        quickStartUpdate.categories.let { categories ->
            assertThat(categories).hasSize(2)
            assertThat(categories[0].taskType).isEqualTo(CUSTOMIZE)
            assertThat(categories[0].uncompletedTasks).containsExactly(CREATE_SITE_TUTORIAL)
            assertThat(categories[0].completedTasks).containsExactly(QuickStartTaskDetails.UPDATE_SITE_TITLE)
            assertThat(categories[1].taskType).isEqualTo(GROW)
            assertThat(categories[1].uncompletedTasks).containsExactly(SHARE_SITE_TUTORIAL)
            assertThat(categories[1].completedTasks).containsExactly(PUBLISH_POST_TUTORIAL)
        }
    }
}
