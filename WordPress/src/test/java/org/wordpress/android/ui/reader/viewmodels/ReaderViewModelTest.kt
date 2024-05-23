package org.wordpress.android.ui.reader.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTag.DISCOVER_PATH
import org.wordpress.android.models.ReaderTag.FOLLOWING_PATH
import org.wordpress.android.models.ReaderTag.LIKED_PATH
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.READER
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.LoadReaderItemsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.utils.ReaderTopBarMenuHelper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.QuickStartReaderPrompt
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.TopBarUiState
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig
import org.wordpress.android.viewmodel.Event
import java.util.Date

private const val DUMMY_CURRENT_TIME: Long = 10000000000

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReaderViewModel

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var dateProvider: DateProvider

    @Mock
    lateinit var loadReaderItemsUseCase: LoadReaderItemsUseCase

    @Mock
    lateinit var readerTracker: ReaderTracker

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var quickStartType: QuickStartType

    @Mock
    lateinit var snackbarSequencer: SnackbarSequencer

    @Mock
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig

    private val emptyReaderTagList = ReaderTagList()
    private val nonEmptyReaderTagList = createNonMockedNonEmptyReaderTagList()

    private val urlUtilsWrapper = UrlUtilsWrapper()

    @Before
    fun setup() {
        viewModel = ReaderViewModel(
            testDispatcher(),
            testDispatcher(),
            appPrefsWrapper,
            dateProvider,
            loadReaderItemsUseCase,
            readerTracker,
            accountStore,
            quickStartRepository,
            selectedSiteRepository,
            jetpackBrandingUtils,
            snackbarSequencer,
            jetpackFeatureRemovalOverlayUtil,
            ReaderTopBarMenuHelper(readerTagsFeedFeatureConfig),
            urlUtilsWrapper,
            readerTagsFeedFeatureConfig,
        )

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(null)
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL))
            .thenReturn(QuickStartNewSiteTask.FOLLOW_SITE)
    }

    @Test
    fun `updateTags invoked on reader tab content is first displayed`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(-1)
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `updateTags NOT invoked if lastUpdate within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD + 1)
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNull()
    }

    @Test
    fun `updateTags invoked if lastUpdate NOT within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD - 1)
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `UiState is NOT updated with content state when loaded tags are empty`() = test {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        whenever(loadReaderItemsUseCase.load()).thenReturn(ReaderTagList())
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(state).isNull()
    }

    @Test
    fun `UiState is updated with content state when loaded tags are NOT empty`() = testWithNonEmptyTags {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(state).isInstanceOf(ContentUiState::class.java)
    }

    @Test
    fun `Tags are reloaded when FollowedTagsFetched event is received`() = testWithNonEmptyTags {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        viewModel.onTagsUpdated(mock())
        // Assert
        assertThat(state).isInstanceOf(ContentUiState::class.java)
    }

    @Test
    fun `Last selected tab is stored into shared preferences`() {
        // Arrange
        val selectedTag: ReaderTag = mock()
        // Act
        viewModel.onTagChanged(selectedTag)
        // Assert
        verify(appPrefsWrapper).setReaderTag(any())
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `Last selected tab is restored after restart`() = testWithNonEmptyTags {
//        // Arrange
//        var tabNavigation: TabNavigation? = null
//        viewModel.selectTab.observeForever {
//            tabNavigation = it.getContentIfNotHandled()
//        }
//        // Act
//        triggerReaderTabContentDisplay(selectedTabReaderTag = nonEmptyReaderTagList[3])
//        // Assert
//        assertThat(tabNavigation!!.position).isEqualTo(3)
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `SelectTab is invoked when last selected tab is null`() = testWithNonMockedNonEmptyTags {
//        // Arrange
//        var tabNavigation: TabNavigation? = null
//        viewModel.selectTab.observeForever {
//            tabNavigation = it.getContentIfNotHandled()
//        }
//        // Act
//        triggerReaderTabContentDisplay(selectedTabReaderTag = null)
//        // Assert
//        assertThat(tabNavigation!!.position).isGreaterThan(-1)
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `SelectTab when tags are empty`() = testWithEmptyTags {
//        // Arrange
//        var tabNavigation: TabNavigation? = null
//        viewModel.selectTab.observeForever {
//            tabNavigation = it.getContentIfNotHandled()
//        }
//        // Act
//        triggerReaderTabContentDisplay()
//        // Assert
//        assertThat(tabNavigation).isNull()
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `Position is changed when selectedTabChange`() = test {
//        // Arrange
//        val tagList = createNonMockedNonEmptyReaderTagList()
//        val readerTag = tagList[2]
//
//        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(tagList)
//
//        viewModel.uiState.observeForever { }
//
//        var tabNavigation: TabNavigation? = null
//        viewModel.selectTab.observeForever {
//            tabNavigation = it.getContentIfNotHandled()
//        }
//
//        // Act
//        triggerReaderTabContentDisplay()
//        viewModel.selectedMenuItemChange(readerTag)
//
//        // Assert
//        assertThat(tabNavigation!!.position).isEqualTo(2)
    }

    @Test
    fun `OnSearchActionClicked emits showSearch event`() {
        // Arrange
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.showSearch.observeForever {
            event = it
        }
        // Act
        viewModel.onSearchActionClicked()

        // Assert
        assertThat(event).isNotNull
    }

    @Test
    fun `Search is disabled for self-hosted login`() = testWithNonEmptyTags {
        // Arrange
        var state: TopBarUiState? = null
        viewModel.topBarUiState.observeForever {
            state = it
        }
        // Act
        triggerContentDisplay(hasAccessToken = false)

        // Assert
        assertThat(state!!.isSearchActionVisible).isFalse
    }

    @Test
    fun `Search is enabled for dot com login`() = testWithNonEmptyTags {
        // Arrange
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var state: TopBarUiState? = null
        viewModel.topBarUiState.observeForever {
            state = it
        }
        // Act
        triggerContentDisplay()

        // Assert
        assertThat(state!!.isSearchActionVisible).isTrue
    }

    @Test
    fun `Tab layout is visible when loaded tags are NOT empty`() = testWithNonEmptyTags {
        // Arrange
        val uiStates = mutableListOf<ReaderUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isInstanceOf(ContentUiState::class.java)
        assertThat((uiStates[0] as ContentUiState).tabLayoutVisible).isTrue
    }

    @Test
    fun `App bar is expanded when loaded tags are NOT empty`() = testWithNonEmptyTags {
        // Arrange
        val uiStates = mutableListOf<ReaderUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        // Act
        triggerContentDisplay()
        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isInstanceOf(ContentUiState::class.java)
        assertThat((uiStates[0] as ContentUiState).appBarExpanded).isTrue
    }

    @Test
    fun `Choose interests screen closed when onCloseReaderInterests is invoked`() = testWithNonEmptyTags {
        // Act
        viewModel.onCloseReaderInterests()
        // Assert
        assertThat(viewModel.closeReaderInterests.value).isNotNull
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `Bookmark tab is selected when bookmarkTabRequested is invoked`() = testWithNonMockedNonEmptyTags {
//        // Arrange
//        var tabNavigation: TabNavigation? = null
//        viewModel.uiState.observeForever {}
//        viewModel.selectTab.observeForever {
//            tabNavigation = it.getContentIfNotHandled()
//        }
//        // Act
//        triggerReaderTabContentDisplay()
//        viewModel.bookmarkTabRequested()
//        // Assert
//        assertThat(tabNavigation!!.position).isEqualTo(3)
    }

    /* QUICK START - FOLLOW SITE TASK EVENT RECEIVED */

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `given discover tab not selected, when qs event is follow site, then discover tab auto selected`() {
//        val tagList = createNonMockedNonEmptyReaderTagList()
//        testWithNonMockedNonEmptyTags {
//            val observers = initObservers()
//            triggerReaderTabContentDisplay(selectedTabReaderTag = tagList[0])
//
//            viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartNewSiteTask.FOLLOW_SITE))
//
//            assertThat(observers.tabNavigationEvents.last().position).isEqualTo(1) // Discover tab index: 1
//        }
    }

    // TODO RenanLukas: update unit tests considering the new VM logic
    @Ignore("Will be updated considering the new VM logic")
    @Test
    fun `given discover tab not selected, when qs event not follow site, then qs discover tab not auto selected`() {
//        val tagList = createNonMockedNonEmptyReaderTagList()
//        testWithNonMockedNonEmptyTags() {
//            val observers = initObservers()
//            triggerReaderTabContentDisplay(selectedTabReaderTag = tagList[0])
//
//            viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartNewSiteTask.CHECK_STATS))
//
//            assertThat(observers.tabNavigationEvents.last().position).isNotEqualTo(1) // Discover tab index: 1
//        }
    }

    @Test
    fun `given reader selected, when qs event follow site, then qs task started and completed`() {
        val tagList = createNonMockedNonEmptyReaderTagList()
        testWithNonMockedNonEmptyTags(tagList) {
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
            whenever(quickStartRepository.isPendingTask(QuickStartNewSiteTask.FOLLOW_SITE)).thenReturn(true)

            val observers = initObservers()
            triggerContentDisplay(hasAccessToken = true)

            viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartNewSiteTask.FOLLOW_SITE))

            assertQsFollowSiteTaskStarted(observers, isSettingsSupported = true)
            assertQsFollowSiteTaskCompleted()
        }
    }

    @Test
    fun `given reader selected no settings available, when qs event follow site, then qs task started and completed`() {
        val tagList = createNonMockedNonEmptyReaderTagList()
        testWithNonMockedNonEmptyTags(tagList) {
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
            whenever(quickStartRepository.isPendingTask(QuickStartNewSiteTask.FOLLOW_SITE)).thenReturn(true)

            val observers = initObservers()
            triggerContentDisplay(hasAccessToken = false)

            viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartNewSiteTask.FOLLOW_SITE))

            assertQsFollowSiteTaskStarted(observers, isSettingsSupported = false)
            assertQsFollowSiteTaskCompleted()
        }
    }

    @Test
    fun `given reader selected, when quick start event not follow site, then qs task not started`() {
        val tagList = createNonMockedNonEmptyReaderTagList()
        testWithNonMockedNonEmptyTags(tagList) {
            val observers = initObservers()
            triggerContentDisplay()

            viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartNewSiteTask.CHECK_STATS))

            assertQsFollowSiteTaskNotStarted(observers)
        }
    }

    @Ignore("Disabled until next sprint")
    @Test
    fun `given wp app, when jp powered bottom sheet feature is true, then jp powered bottom sheet is shown`() {
        val showJetpackPoweredBottomSheetEvent = mutableListOf<Event<Boolean>>()
        viewModel.showJetpackPoweredBottomSheet.observeForever {
            showJetpackPoweredBottomSheetEvent.add(it)
        }
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(true)

        viewModel.start()

        assertThat(showJetpackPoweredBottomSheetEvent.last().peekContent()).isEqualTo(true)
    }

    @Test
    fun `given wp app, when jp bottom sheet feature is false, then bottom sheet is not shown`() = testWithEmptyTags {
        val showJetpackPoweredBottomSheetEvent = mutableListOf(Event(false))
        viewModel.showJetpackPoweredBottomSheet.observeForever {
            showJetpackPoweredBottomSheetEvent.add(it)
        }
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(false)

        viewModel.start()

        assertThat(showJetpackPoweredBottomSheetEvent.last().peekContent()).isEqualTo(false)
    }

    @Test
    fun `given wp app, when jetpack overlay feature is false, then jp fullscreen overlay is not shown`() {
        val showJetpackOverlayEvent = mutableListOf(Event(false))
        viewModel.showJetpackOverlay.observeForever {
            showJetpackOverlayEvent.add(it)
        }
        whenever(jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(READER)).thenReturn(false)

        viewModel.onScreenInForeground()

        assertThat(showJetpackOverlayEvent.last().peekContent()).isFalse
    }

    @Test
    fun `given wp app, when jetpack overlay feature is true, then jp fullscreen overlay is shown`() {
        val showJetpackOverlayEvent = mutableListOf(Event(false))
        viewModel.showJetpackOverlay.observeForever {
            showJetpackOverlayEvent.add(it)
        }
        whenever(jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(READER)).thenReturn(true)

        viewModel.onScreenInForeground()

        assertThat(showJetpackOverlayEvent.last().peekContent()).isTrue
    }

    private fun assertQsFollowSiteTaskStarted(
        observers: Observers,
        isSettingsSupported: Boolean = true
    ) {
        with(observers) {
            assertThat(quickStartReaderPrompts.last().peekContent().shortMessagePrompt).isEqualTo(
                if (isSettingsSupported) {
                    R.string.quick_start_dialog_follow_sites_message_short_discover_and_subscriptions
                } else {
                    R.string.quick_start_dialog_follow_sites_message_short_discover
                }
            )
        }
    }

    private fun assertQsFollowSiteTaskNotStarted(
        observers: Observers
    ) {
        with(observers) {
            assertThat(quickStartReaderPrompts).isEmpty()
        }
    }

    private fun assertQsFollowSiteTaskCompleted() {
        verify(quickStartRepository).completeTask(QuickStartNewSiteTask.FOLLOW_SITE)
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<ReaderUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }

        val quickStartReaderPrompts = mutableListOf<Event<QuickStartReaderPrompt>>()
        viewModel.quickStartPromptEvent.observeForever {
            quickStartReaderPrompts.add(it)
        }

        val tabNavigationEvents = mutableListOf<TabNavigation>()
        // TODO RenanLukas: update unit tests considering the new VM logic
//        viewModel.selectTab.observeForever {
//            tabNavigationEvents.add(it.peekContent())
//        }

        return Observers(uiStates, quickStartReaderPrompts, tabNavigationEvents)
    }

    private data class Observers(
        val uiStates: List<ReaderUiState>,
        val quickStartReaderPrompts: List<Event<QuickStartReaderPrompt>>,
        val tabNavigationEvents: List<TabNavigation>,
    )

    private fun triggerContentDisplay(
        hasAccessToken: Boolean = true
    ) {
        whenever(accountStore.hasAccessToken()).thenReturn(hasAccessToken)
        viewModel.start()
    }

    private fun <T> testWithEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderItemsUseCase.load()).thenReturn(emptyReaderTagList)
            block()
        }
    }

    private fun <T> testWithNonEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderItemsUseCase.load()).thenReturn(nonEmptyReaderTagList)
            block()
        }
    }

    private fun <T> testWithNonMockedNonEmptyTags(
        readerTags: ReaderTagList = createNonMockedNonEmptyReaderTagList(),
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            whenever(loadReaderItemsUseCase.load()).thenReturn(readerTags)
            block()
        }
    }

    private fun createNonMockedNonEmptyReaderTagList(): ReaderTagList {
        return ReaderTagList().apply {
            add(ReaderTag("Following", "Following", "Following", FOLLOWING_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Discover", "Discover", "Discover", DISCOVER_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Like", "Like", "Like", LIKED_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Saved", "Saved", "Saved", "Saved", ReaderTagType.BOOKMARKED))
        }
    }
}
