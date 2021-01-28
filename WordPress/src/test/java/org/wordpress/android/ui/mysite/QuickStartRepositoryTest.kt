package org.wordpress.android.ui.mysite

import android.text.SpannableString
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.CREATE_SITE_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.PUBLISH_POST_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.SHARE_SITE_TUTORIAL
import org.wordpress.android.ui.utils.UiString.UiStringText
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
    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var source: Flow<QuickStartUpdate>
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
                dispatcher
        )
        snackbars = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        site = SiteModel()
        site.id = siteId
        source = quickStartRepository.buildSource(siteId)
    }

    @Test
    fun `refresh loads model`() = test {
        initStore()

        quickStartRepository.refreshIfNecessary()

        assertModel(2)
    }

    @Test
    fun `start marks CREATE_SITE as done and loads model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.startQuickStart()

        verify(quickStartStore).setDoneTask(siteId.toLong(), CREATE_SITE, true)
        assertModel(2)
    }

    @Test
    fun `sets active task and shows stylized snackbar when not UPDATE_SITE_TITLE`() = test {
        initStore()
        quickStartRepository.refreshIfNecessary()

        val spannableString = initActiveTask(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)

        quickStartRepository.setActiveTask(PUBLISH_POST)

        assertThat(source.take(3).toList().last().quickStartModel.activeTask).isEqualTo(PUBLISH_POST)
        assertThat((snackbars.last().message as UiStringText).text).isEqualTo(spannableString)
    }

    private fun initActiveTask(quickStartMySitePrompts: QuickStartMySitePrompts): SpannableString {
        val spannableString = mock<SpannableString>()
        whenever(
                quickStartUtils.stylizeQuickStartPrompt(
                        eq(quickStartMySitePrompts.shortMessagePrompt),
                        eq(quickStartMySitePrompts.iconId)
                )
        ).thenReturn(spannableString)
        return spannableString
    }

    @Test
    fun `completeTask marks current active task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        val task = PUBLISH_POST

        initActiveTask(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
        quickStartRepository.setActiveTask(task)

        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteId.toLong(), task, true)
        val update = source.take(2).toList().last()
        assertThat(update.quickStartModel.activeTask).isNull()
        assertThat(update.quickStartModel.categories).isNotEmpty()
    }

    @Test
    fun `completeTask does not marks active task as done if it is different`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        initActiveTask(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
        quickStartRepository.setActiveTask(PUBLISH_POST)

        quickStartRepository.completeTask(UPDATE_SITE_TITLE)

        verifyZeroInteractions(quickStartStore)
    }

    private fun initQuickStartInProgress() {
        initStore()
        quickStartRepository.refreshIfNecessary()
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

    private suspend fun assertModel(elements: Int = 1) {
        val quickStartUpdate = source.take(elements).toList().last()
        quickStartUpdate.quickStartModel.categories.let { categories ->
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
