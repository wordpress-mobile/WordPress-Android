package org.wordpress.android.ui.mysite

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
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.CREATE_SITE_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.PUBLISH_POST_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.SHARE_SITE_TUTORIAL
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

class QuickStartRepositoryTest : BaseUnitTest() {
    @Mock lateinit var quickStartStore: QuickStartStore
    @Mock lateinit var quickStartUtils: QuickStartUtilsWrapper
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var eventBus: EventBusWrapper
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var result: MutableList<QuickStartUpdate>
    private val siteId = 1

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        quickStartRepository = QuickStartRepository(
                TEST_DISPATCHER,
                quickStartStore,
                quickStartUtils,
                selectedSiteRepository,
                resourceProvider,
                analyticsTrackerWrapper,
                dispatcher,
                eventBus,
                appPrefsWrapper
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
        site.id = siteId
        result = mutableListOf()
        quickStartRepository.buildSource(TEST_SCOPE, siteId).observeForever { result.add(it) }
    }

    @Test
    fun `refresh loads model`() = test {
        initStore()

        quickStartRepository.refresh()

        assertModel()
    }

    @Test
    fun `start marks CREATE_SITE as done and loads model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.startQuickStart()

        verify(quickStartStore).setDoneTask(siteId.toLong(), CREATE_SITE, true)
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
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)

        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteId.toLong(), task, true)
        val update = result.last()
        assertThat(update.activeTask).isNull()
        assertThat(update.categories).isNotEmpty()
    }

    @Test
    fun `completeTask marks current pending task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)
        quickStartRepository.requestNextStepOfTask(task)
        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteId.toLong(), task, true)
        val update = result.last()
        assertThat(update.activeTask).isNull()
        assertThat(update.categories).isNotEmpty()
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
    fun `requestNextStepOfTask emits quick start event`() {
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
        val updatedSiteId = 2
        site.id = updatedSiteId
        site.showOnFront = ShowOnFront.POSTS.value
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartStore.hasDoneTask(updatedSiteId.toLong(), EDIT_HOMEPAGE)).thenReturn(false)

        quickStartRepository.buildSource(TEST_SCOPE, updatedSiteId)

        verify(quickStartStore).setDoneTask(updatedSiteId.toLong(), EDIT_HOMEPAGE, true)
    }

    @Test
    fun `does not mark EDIT_HOMEPAGE task as done when site showing Homepage`() = test {
        val updatedSiteId = 2
        site.id = updatedSiteId
        site.showOnFront = ShowOnFront.PAGE.value
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        quickStartRepository.buildSource(TEST_SCOPE, updatedSiteId)

        verify(quickStartStore, never()).setDoneTask(updatedSiteId.toLong(), EDIT_HOMEPAGE, true)
    }

    @Test
    fun `hides CUSTOMIZE category`() = test {
        initStore()

        quickStartRepository.hideCategory(CUSTOMIZE_QUICK_START)

        val quickStartUpdate = result.last()
        quickStartUpdate.categories.apply {
            assertThat(this).hasSize(1)
            assertThat(this.first().taskType).isEqualTo(GROW)
        }
    }

    @Test
    fun `hides GROW category`() = test {
        initStore()

        quickStartRepository.hideCategory(GROW_QUICK_START)

        val quickStartUpdate = result.last()
        quickStartUpdate.categories.apply {
            assertThat(this).hasSize(1)
            assertThat(this.first().taskType).isEqualTo(CUSTOMIZE)
        }
    }

    @Test
    fun `removes CUSTOMIZE category`() = test {
        initStore()

        quickStartRepository.removeCategory(CUSTOMIZE_QUICK_START)

        val quickStartUpdate = result.last()
        quickStartUpdate.categories.apply {
            assertThat(this).hasSize(1)
            assertThat(this.first().taskType).isEqualTo(GROW)
        }

        verify(appPrefsWrapper).removeQuickStartTaskType(CUSTOMIZE)
    }

    @Test
    fun `removes GROW category`() = test {
        initStore()

        quickStartRepository.removeCategory(GROW_QUICK_START)

        val quickStartUpdate = result.last()
        quickStartUpdate.categories.apply {
            assertThat(this).hasSize(1)
            assertThat(this.first().taskType).isEqualTo(CUSTOMIZE)
        }
    }

    private fun initQuickStartInProgress() {
        initStore()
        quickStartRepository.refresh()
    }

    private fun initStore() {
        whenever(quickStartUtils.isQuickStartInProgress(site.id)).thenReturn(true)
        whenever(quickStartStore.getUncompletedTasksByType(siteId.toLong(), CUSTOMIZE)).thenReturn(listOf(CREATE_SITE))
        whenever(quickStartStore.getCompletedTasksByType(siteId.toLong(), CUSTOMIZE)).thenReturn(
                listOf(
                        UPDATE_SITE_TITLE
                )
        )
        whenever(
                quickStartStore.getUncompletedTasksByType(
                        siteId.toLong(),
                        GROW
                )
        ).thenReturn(listOf(ENABLE_POST_SHARING))
        whenever(quickStartStore.getCompletedTasksByType(siteId.toLong(), GROW)).thenReturn(listOf(PUBLISH_POST))
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
