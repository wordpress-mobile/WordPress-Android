package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DateUtilsWrapper
import org.wordpress.android.ui.posts.PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent
import org.wordpress.android.ui.utils.UiString

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostResolutionOverlayViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var dateUtilsWrapper: DateUtilsWrapper

    @Mock
    lateinit var tracker: PostResolutionOverlayAnalyticsTracker

    lateinit var viewModel: PostResolutionOverlayViewModel

    private lateinit var dismissDialog: MutableList<Boolean>
    private lateinit var uiStates: MutableList<PostResolutionOverlayUiState>
    private lateinit var triggerListener: MutableList<PostResolutionConfirmationEvent>

    private val postModel = PostModel().apply {
        setIsPage(false)
        setDateLocallyChanged("2024-05-24")
        setLastModified("2024-05-25")
        setRemoteLastModified("2024-04-12")
        setAutoSaveModified("2025-01-10")
        setId(1)
    }

    private val selectedContentItem = ContentItem(
        ContentItemType.LOCAL_DEVICE,
        1,
        1,
        UiString.UiStringText("date"),
        true)

    private val unSelectedContentItem = ContentItem(
        ContentItemType.LOCAL_DEVICE,
        1,
        1,
        UiString.UiStringText("date"),
        false)

    @Before
    fun setUp() = test {
        viewModel = PostResolutionOverlayViewModel(dateTimeUtilsWrapper, dateUtilsWrapper, tracker)
        dismissDialog = mutableListOf()
        uiStates = mutableListOf()
        triggerListener = mutableListOf()
        launch(testDispatcher()) {
            viewModel.dismissDialog.observeForever {
                dismissDialog.add(it)
            }

            viewModel.uiState.observeForever {
                uiStates.add(it)
            }

            viewModel.triggerListeners.observeForever {
                triggerListener.add(it)
            }
        }
        whenever(dateTimeUtilsWrapper.timestampFromIso8601Millis(any())).thenReturn(1)
        whenever(dateUtilsWrapper.formatDateTime(any(), any())).thenReturn("Monday, Dec 24 at 10:25")
    }

    @Test
    fun `given post model is null, when start, then dialog dismiss event is posted`() {
        viewModel.start(null, PostResolutionType.SYNC_CONFLICT)

        assertThat(dismissDialog.last()).isTrue()
    }

    @Test
    fun `given post resolution type is null, when start, then dialog dismiss event is posted`() {
        viewModel.start(PostModel(), null)

        assertThat(dismissDialog.last()).isTrue()
    }

    @Test
    fun `given sync conflict request, when start for SYNC_CONFLICT, then uiState is built`() {
        viewModel.start(postModel, PostResolutionType.SYNC_CONFLICT)

        val uiState = uiStates.last()
        assertThat(uiState).isNotNull
        assertThat(uiState.content).isNotNull
        assertThat(uiState.content.size).isEqualTo(2)
    }

    @Test
    fun `given sync conflict request, when start for AUTOSAVE_REVISION_CONFLICT, then uiState is built`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        assertThat(uiState).isNotNull
        assertThat(uiState.content).isNotNull
        assertThat(uiState.content.size).isEqualTo(2)
    }

    @Test
    fun `when on close click, then dialog dismiss event is posted`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.closeClick.invoke()

        assertThat(dismissDialog.last()).isTrue()
    }

    @Test
    fun `when on close click, then close is tracked`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.closeClick.invoke()

        verify(tracker, times(1)).trackClose(any(), any())
    }

    @Test
    fun `when on cancel click, then dialog dismiss event is posted`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.cancelClick.invoke()

        assertThat(dismissDialog.last()).isTrue()
    }

    @Test
    fun `when on cancel click, then cancel is tracked`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.cancelClick.invoke()

        verify(tracker, times(1)).trackCancel(any(), any())
    }

    @Test
    fun `when on dialog dismissed click, then dialog dismiss is tracked`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        viewModel.onDialogDismissed()

        verify(tracker, times(1)).trackDismissed(any(), any())
    }

    @Test
    fun `when on confirm click, then dialog dismiss event is posted`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.confirmClick.invoke()

        assertThat(dismissDialog.last()).isTrue()
    }

    @Test
    fun `when on confirm click, then confirm is tracked`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.onSelected.invoke(selectedContentItem)
        uiState.confirmClick.invoke()

        verify(tracker, times(1)).trackConfirm(any(), any(), any())
    }

    @Test
    fun `when item is selected, then uiState is update with selectedContentItem`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        assertThat(uiState.selectedContentItem).isNull()

        uiState.onSelected.invoke(selectedContentItem)

        val selectedContentItem = uiStates.last().selectedContentItem
        assertThat(selectedContentItem).isNotNull
    }

    @Test
    fun `given no selected item, when on confirm click, then no events are posted to trigger listeners`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.confirmClick.invoke()

        assertThat(triggerListener).isEmpty()
    }

    @Test
    fun `given selected item, when on confirm click, then event is posted to trigger listeners`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.onSelected.invoke(selectedContentItem)
        uiState.confirmClick.invoke()

        assertThat(triggerListener.last()).isInstanceOf(PostResolutionConfirmationEvent::class.java)
    }


    @Test
    fun `given autosave revision conflict, when on confirm click, then event posted is for autosave`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        uiState.onSelected.invoke(selectedContentItem)
        uiState.confirmClick.invoke()

        val event = triggerListener.last()
        assertThat(event).isInstanceOf(PostResolutionConfirmationEvent::class.java)
        assertThat(event.postResolutionType).isEqualTo(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
    }

    @Test
    fun `given sync conflict, when on confirm click, then event posted is for sync conflict`() {
        viewModel.start(postModel, PostResolutionType.SYNC_CONFLICT)

        val uiState = uiStates.last()
        uiState.onSelected.invoke(selectedContentItem)
        uiState.confirmClick.invoke()

        val event = triggerListener.last()
        assertThat(event).isInstanceOf(PostResolutionConfirmationEvent::class.java)
        assertThat(event.postResolutionType).isEqualTo(PostResolutionType.SYNC_CONFLICT)
    }

    @Test
    fun `when item is selected, then uiState actionEnabled is updated to true`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        assertThat(uiState.actionEnabled).isFalse()
        uiState.onSelected.invoke(selectedContentItem)

        assertThat(uiStates.last().actionEnabled).isTrue()
    }

    @Test
    fun `when item is deselected, then uiState actionEnabled is updated to false`() {
        viewModel.start(postModel, PostResolutionType.AUTOSAVE_REVISION_CONFLICT)

        val uiState = uiStates.last()
        assertThat(uiState.actionEnabled).isFalse()
        uiState.onSelected.invoke(unSelectedContentItem)

        assertThat(uiStates.last().actionEnabled).isFalse()
    }
}
