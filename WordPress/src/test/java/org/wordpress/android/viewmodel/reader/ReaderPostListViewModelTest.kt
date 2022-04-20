package org.wordpress.android.viewmodel.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel.QuickStartReaderPrompt
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostListViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var readerSeenStatusToggleUseCase: ReaderSeenStatusToggleUseCase
    @Mock private lateinit var quickStartRepository: QuickStartRepository
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var viewModel: ReaderPostListViewModel

    @Before
    fun setUp() {
        viewModel = ReaderPostListViewModel(
                readerPostCardActionsHandler,
                reblogUseCase,
                readerTracker,
                readerSeenStatusToggleUseCase,
                quickStartRepository,
                selectedSiteRepository,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    /* QUICK START */

    @Test
    fun `when quick start follow site task event received, then follow site setting step prompt shown`() {
        val observers = initObservers()

        viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartTask.FOLLOW_SITE))

        val quickStartReaderPrompt = observers.quickStartReaderPrompts.last().peekContent()
        assertThat(quickStartReaderPrompt).isEqualTo(QuickStartReaderPrompt.FollowSiteSettingsStepPrompt)
    }

    @Test
    fun `when quick start non follow site event received, then follow site setting step prompt not shown`() {
        val observers = initObservers()

        viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartTask.CHECK_STATS))

        assertThat(observers.quickStartReaderPrompts).isEmpty()
    }

    @Test
    fun `given site present, when quick start follow site event received, then follow site task is completed`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())

        viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartTask.FOLLOW_SITE))

        verify(quickStartRepository).completeTask(QuickStartTask.FOLLOW_SITE)
    }

    @Test
    fun `given site present, when quick start non follow site event received, then follow site task not completed`() {
        viewModel.onQuickStartEventReceived(QuickStartEvent(QuickStartTask.CHECK_STATS))

        verify(quickStartRepository, times(0)).completeTask(QuickStartTask.FOLLOW_SITE)
    }

    private fun initObservers(): Observers {
        val quickStartReaderPrompts = mutableListOf<Event<QuickStartReaderPrompt>>()
        viewModel.quickStartPromptEvent.observeForever {
            quickStartReaderPrompts.add(it)
        }

        return Observers(quickStartReaderPrompts)
    }

    private data class Observers(
        val quickStartReaderPrompts: List<Event<QuickStartReaderPrompt>>
    )
}
