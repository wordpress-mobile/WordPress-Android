package org.wordpress.android.ui.mysite.cards.quickstart

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardsModel
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.CREATE_SITE_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.PUBLISH_POST_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.SHARE_SITE_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.QuickStartExistingUsersV2FeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class QuickStartCardSourceTest : BaseUnitTest() {
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
    @Mock lateinit var quickStartTracker: QuickStartTracker
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var quickStartCardSource: QuickStartCardSource
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var result: MutableList<QuickStartUpdate>
    private val siteLocalId = 1
    private val quickStartType = NewSiteQuickStartType

    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() = test {
        site = SiteModel()
        site.id = siteLocalId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(appPrefsWrapper.getLastSelectedQuickStartTypeForSite(any())).thenReturn(NewSiteQuickStartType)
        whenever(quickStartExistingUsersV2FeatureConfig.isEnabled()).thenReturn(false)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        quickStartRepository = QuickStartRepository(
                testDispatcher(),
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
        quickStartCardSource = QuickStartCardSource(
                quickStartRepository,
                quickStartStore,
                quickStartUtilsWrapper,
                selectedSiteRepository)
        snackbars = mutableListOf()
        quickStartPrompts = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        quickStartRepository.onQuickStartMySitePrompts.observeForever { event ->
            event?.getContentIfNotHandled()?.let { quickStartPrompts.add(it) }
        }
        result = mutableListOf()
        isRefreshing = mutableListOf()
        quickStartCardSource.refresh.observeForever { isRefreshing.add(it) }
    }

    @Test
    fun `refresh loads model`() = test {
        initStore()

        quickStartCardSource.refresh()

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
        quickStartCardSource.refresh()

        assertThat(snackbars).isEmpty()
    }

    @Test
    fun `start marks CREATE_SITE as done and loads model`() = test {
        initStore()

        quickStartCardSource.build(testScope(), site.id)
        quickStartCardSource.refresh()

        assertModel()
    }

    @Test
    fun `sets active task and shows stylized snackbar when not UPDATE_SITE_TITLE`() = test {
        initStore()
        quickStartCardSource.refresh()

        quickStartRepository.setActiveTask(PUBLISH_POST)

        assertThat(result.last().activeTask).isEqualTo(PUBLISH_POST)
        assertThat(quickStartPrompts.last()).isEqualTo(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
    }

    @Test
    fun `completeTask marks current active task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        quickStartCardSource.refresh()
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
        quickStartCardSource.refresh()
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

        verifyNoInteractions(eventBus)
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
    fun `given uncompleted task, when quick start notice button action is clicked, then the task is marked active`() =
            test {
                initStore(nextUncompletedTask = PUBLISH_POST)
                quickStartRepository.checkAndShowQuickStartNotice()

                snackbars.last().buttonAction.invoke()

                assertThat(result.last().activeTask).isEqualTo(PUBLISH_POST)
            }

    @Test
    fun `when source is invoked, then refresh is false`() = test {
        initBuild()

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        quickStartCardSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        initStore()

        val updatedSiteId = 2
        site.id = updatedSiteId
        site.showOnFront = ShowOnFront.POSTS.value
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        quickStartCardSource.refresh.observeForever { isRefreshing.add(it) }

        quickStartCardSource.build(testScope(), site.id)

        quickStartCardSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `given quick start available for site, when source is refreshed, then non empty categories returned`() = test {
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        initStore()

        quickStartCardSource.refresh()

        val update = result.last()
        assertThat(update.categories).isNotEmpty
    }

    @Test
    fun `given quick start not available for site, when source is refreshed, then empty categories returned`() = test {
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
        initStore()

        quickStartCardSource.refresh()

        val update = result.last()
        assertThat(update.categories).isEmpty()
    }

    private fun triggerQSRefreshAfterSameTypeTasksAreComplete() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isEveryQuickStartTaskDoneForType(siteLocalId, GROW)).thenReturn(true)
        whenever(resourceProvider.getString(any())).thenReturn(ALL_TASKS_COMPLETED_MESSAGE)
        whenever(htmlCompat.fromHtml(ALL_TASKS_COMPLETED_MESSAGE)).thenReturn(ALL_TASKS_COMPLETED_MESSAGE)

        val task = PUBLISH_POST
        quickStartRepository.setActiveTask(task)
        quickStartRepository.completeTask(task)
        quickStartCardSource.refresh()
    }

    private suspend fun initQuickStartInProgress() {
        initStore()
        quickStartCardSource.refresh()
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
        whenever(quickStartType.isQuickStartInProgress(quickStartStore, siteLocalId.toLong())).thenReturn(true)
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
        whenever(quickStartUtilsWrapper.getNextUncompletedQuickStartTask(quickStartType, siteLocalId.toLong()))
                .thenReturn(nextUncompletedTask)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormat(anyOrNull())).thenReturn("")
        initBuild()
    }

    private fun initBuild() {
        quickStartCardSource.build(testScope(), siteLocalId).observeForever { result.add(it) }
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

    companion object {
        const val ALL_TASKS_COMPLETED_MESSAGE = "All tasks completed!"
    }
}
