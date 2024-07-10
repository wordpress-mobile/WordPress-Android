package org.wordpress.android.ui.mysite.cards.quickstart

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class QuickStartCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var quickStartStore: QuickStartStore

    @Mock
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var eventBus: EventBusWrapper

    @Mock
    lateinit var htmlCompat: HtmlCompatWrapper

    @Mock
    lateinit var contextProvider: ContextProvider

    @Mock
    lateinit var htmlMessageUtils: HtmlMessageUtils

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var quickStartTracker: QuickStartTracker

    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var quickStartCardBuilder: QuickStartCardBuilder

    private lateinit var site: SiteModel
    private lateinit var quickStartRepository: QuickStartRepository

    private lateinit var mQuickStartCardViewModelSlice: QuickStartCardViewModelSlice

    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var quickStartPrompts: MutableList<QuickStartMySitePrompts>
    private lateinit var result: MutableList<MySiteCardAndItem.Card.QuickStartCard?>
    private val siteLocalId = 1
    private val quickStartType = NewSiteQuickStartType

    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() = test {
        site = SiteModel()
        site.id = siteLocalId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(appPrefsWrapper.getLastSelectedQuickStartTypeForSite(any())).thenReturn(
            NewSiteQuickStartType
        )
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
            htmlCompat,
            contextProvider,
            htmlMessageUtils,
            quickStartTracker
        )
        mQuickStartCardViewModelSlice = QuickStartCardViewModelSlice(
            testDispatcher(),
            quickStartRepository,
            quickStartStore,
            quickStartUtilsWrapper,
            selectedSiteRepository,
            cardsTracker,
            quickStartTracker,
            quickStartCardBuilder
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

        result = mutableListOf()
        mQuickStartCardViewModelSlice.uiModel.observeForever { result.add(it) }

        isRefreshing = mutableListOf()
        mQuickStartCardViewModelSlice.isRefreshing.observeForever { isRefreshing.add(it) }

        mQuickStartCardViewModelSlice.initialize(testScope())
    }

    @Test
    fun `refresh loads model`() = test {
        initStore()

        mQuickStartCardViewModelSlice.build(site)
    }

    @Test
    @Ignore("This test fails due to the way it is structured to test the quick start card, repo and store")
    fun `given same type tasks done, when refresh started, then both task types exists`() =
        test {
            initStore()

            triggerQSRefreshAfterSameTypeTasksAreComplete()

            assertThat(result.last()?.taskTypeItems?.map { it.quickStartTaskType }).contains(
                CUSTOMIZE,
                GROW
            )
        }

    @Test
    @Ignore("This test fails due to the way it is structured to test the quick start card, repo and store")
    fun `start marks CREATE_SITE as done and loads model`() = test {
        initStore()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        whenever(quickStartRepository.getQuickStartTaskTypes()).thenReturn(
            listOf(
                CUSTOMIZE,
                GROW
            )
        )
        whenever(quickStartRepository.quickStartType.isQuickStartInProgress(quickStartStore, site.siteId))
            .thenReturn(true)

        quickStartRepository.checkAndSetQuickStartType(true)
        quickStartUtilsWrapper
            .startQuickStart(
                siteLocalId,
                true,
                quickStartRepository.quickStartType,
                quickStartTracker
            )

        mQuickStartCardViewModelSlice.build(site)
        advanceUntilIdle()

        assertThat(result.last()?.quickStartCardType).isEqualTo(QuickStartCardType.GET_TO_KNOW_THE_APP)
    }

    @Test
    @Ignore("This test fails due to the way it is structured to test the quick start card, repo and store")
    fun `sets active task and shows stylized snackbar when not UPDATE_SITE_TITLE`() = test {
        initStore()

        quickStartRepository.setActiveTask(PUBLISH_POST)
        mQuickStartCardViewModelSlice.build(site)

        assertThat(result.last()?.taskTypeItems?.last()).isEqualTo(PUBLISH_POST)
        assertThat(quickStartPrompts.last()).isEqualTo(QuickStartMySitePrompts.PUBLISH_POST_TUTORIAL)
    }

    @Test
    fun `completeTask marks current active task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        mQuickStartCardViewModelSlice.build(site)
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)
        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteLocalId.toLong(), task, true)
        assertNull(result.last())
    }

    @Test
    fun `completeTask marks current pending task as done and refreshes model`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initStore()
        mQuickStartCardViewModelSlice.build(site)
        val task = PUBLISH_POST

        quickStartRepository.setActiveTask(task)
        quickStartRepository.requestNextStepOfTask(task)
        quickStartRepository.completeTask(task)

        verify(quickStartStore).setDoneTask(siteLocalId.toLong(), task, true)
        assertNull(result.last())
    }

    @Test
    @Ignore("This test fails due to the way it is structured to test the quick start card, repo and store")
    fun `given quick start available for site, when source is refreshed, then non empty categories returned`() =
        test {
            whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
            initStore()

            mQuickStartCardViewModelSlice.build(site)

            assertNotNull(result.last())
        }

    @Test
    fun `given quick start not available for site, when source is refreshed, then empty categories returned`() =
        test {
            whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
            initStore()

            mQuickStartCardViewModelSlice.build(site)

            assertNull(result.last())
        }

    private fun triggerQSRefreshAfterSameTypeTasksAreComplete() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        val task = PUBLISH_POST
        quickStartRepository.setActiveTask(task)
        quickStartRepository.completeTask(task)
        mQuickStartCardViewModelSlice.build(site)
    }

    private fun initStore() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(
            quickStartType.isQuickStartInProgress(
                quickStartStore,
                siteLocalId.toLong()
            )
        ).thenReturn(true)
        whenever(
            quickStartStore.getUncompletedTasksByType(
                siteLocalId.toLong(),
                CUSTOMIZE
            )
        ).thenReturn(
            listOf(
                CREATE_SITE
            )
        )
        whenever(
            quickStartStore.getCompletedTasksByType(
                siteLocalId.toLong(),
                CUSTOMIZE
            )
        ).thenReturn(
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
        whenever(quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), GROW)).thenReturn(
            listOf(PUBLISH_POST)
        )
    }
}
