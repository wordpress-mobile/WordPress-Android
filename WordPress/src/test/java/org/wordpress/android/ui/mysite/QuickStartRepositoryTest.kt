package org.wordpress.android.ui.mysite

import android.text.SpannableString
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
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
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartModel
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
    private lateinit var selectedSite: MutableLiveData<SiteModel>
    private lateinit var models: MutableList<QuickStartModel>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private val siteId = 1

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        selectedSite = MutableLiveData()
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(selectedSite)
        quickStartRepository = QuickStartRepository(
                TEST_DISPATCHER,
                quickStartStore,
                quickStartUtils,
                selectedSiteRepository,
                resourceProvider,
                analyticsTrackerWrapper,
                dispatcher
        )
        models = mutableListOf()
        quickStartRepository.quickStartModel.observeForever { if (it != null) models.add(it) }
        snackbars = mutableListOf()
        quickStartRepository.onSnackbar.observeForever { event ->
            event?.getContentIfNotHandled()
                    ?.let { snackbars.add(it) }
        }
        site = SiteModel()
        site.id = siteId
    }

    @Test
    fun `model is empty when not started`() {
        assertEmptyModel()
    }

    @Test
    fun `refresh loads model`() {
        initStore()
        quickStartRepository.refreshIfNecessary()

        assertModel()
    }

    @Test
    fun `start marks CREATE_SITE as done and loads model`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()

        quickStartRepository.startQuickStart()

        verify(quickStartStore).setDoneTask(siteId.toLong(), CREATE_SITE, true)
        assertModel()
    }

    @Test
    fun `sets active task and shows sylized snackbar when not UPDATE_SITE_TITLE`() {
        initStore()
        quickStartRepository.refreshIfNecessary()

        val spannableString = initActiveTask(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)

        quickStartRepository.setActiveTask(PUBLISH_POST)

        assertThat(models.last().activeTask).isEqualTo(PUBLISH_POST)
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
    fun `completeTask marks current active task as done and refreshes model`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        val task = PUBLISH_POST
        assertThat(models).isEmpty()
        initActiveTask(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
        quickStartRepository.setActiveTask(task)

        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteId.toLong(), task, true)
        assertThat(models.last().activeTask).isNull()
        assertThat(models.last().categories).isNotEmpty()
    }

    @Test
    fun `completeTask does not marks active task as done if it is different`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        assertThat(models).isEmpty()
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
        selectedSite.value = site
        whenever(quickStartUtils.isQuickStartInProgress(site)).thenReturn(true)
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
        assertThat(models).hasSize(1)
        models.last().categories.let { categories ->
            assertThat(categories).hasSize(2)
            assertThat(categories[0].taskType).isEqualTo(CUSTOMIZE)
            assertThat(categories[0].uncompletedTasks).containsExactly(CREATE_SITE_TUTORIAL)
            assertThat(categories[0].completedTasks).containsExactly(QuickStartTaskDetails.UPDATE_SITE_TITLE)
            assertThat(categories[1].taskType).isEqualTo(GROW)
            assertThat(categories[1].uncompletedTasks).containsExactly(SHARE_SITE_TUTORIAL)
            assertThat(categories[1].completedTasks).containsExactly(PUBLISH_POST_TUTORIAL)
        }
    }

    private fun assertEmptyModel() {
        models.isEmpty()
    }
}
