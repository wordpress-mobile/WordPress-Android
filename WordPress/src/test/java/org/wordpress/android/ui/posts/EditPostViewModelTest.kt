package org.wordpress.android.ui.posts

import android.content.Context
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor.PostFields
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import java.util.Calendar
import java.util.TimeZone

class EditPostViewModelTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var aztecEditorWrapper: AztecEditorWrapper
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var context: Context
    @Mock lateinit var postRepository: EditPostRepository

    private lateinit var transactionCaptor: KArgumentCaptor<(PostModel) -> Boolean>
    private lateinit var updateResultCaptor: KArgumentCaptor<(PostModel) -> UpdateResult>
    private lateinit var actionCaptor: KArgumentCaptor<Action<PostModel>>

    private lateinit var viewModel: EditPostViewModel
    private val title = "title"
    private val updatedTitle = "updatedTitle"
    private val content = "content"
    private val updatedContent = "updatedContent"
    private val currentTime = "2019-11-10T11:10:00+0100"
    private val postStatus = "DRAFT"
    private val postModel = PostModel()

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = EditPostViewModel(
                TEST_DISPATCHER,
                dispatcher,
                aztecEditorWrapper,
                localeManagerWrapper,
                dateTimeUtilsWrapper
        )
        transactionCaptor = argumentCaptor()
        updateResultCaptor = argumentCaptor()
        actionCaptor = argumentCaptor()
        setupCurrentTime()
        postModel.setTitle(title)
        postModel.setContent(content)
        postModel.setStatus(postStatus)
        whenever(postRepository.getEditablePost()).thenReturn(postModel)
        whenever(postRepository.content).thenReturn(content)
    }

    @Test
    fun `delays save call`() {
        var event: Event<Unit>? = null
        viewModel.onSavePostTriggered.observeForever {
            event = it
        }
        assertThat(event).isNull()

        viewModel.savePost()

        assertThat(event).isNotNull()
    }

    @Test
    fun `sorts media IDs`() {
        viewModel.mediaMarkedUploadingOnStartIds = listOf("B", "A")

        viewModel.sortMediaMarkedUploadingOnStartIds()

        assertThat(viewModel.mediaMarkedUploadingOnStartIds).containsExactly("A", "B")
    }

    @Test
    fun `saves post to DB and updates media IDs for Aztec editor`() {
        assertThat(viewModel.mediaMarkedUploadingOnStartIds).isEmpty()
        val mediaIDs = listOf("A")
        whenever(
                aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        content
                )
        ).thenReturn(
                mediaIDs
        )
        whenever(postRepository.postHasChangesFromDb()).thenReturn(true)

        viewModel.savePostToDb(context, postRepository, true)

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.firstValue.type).isEqualTo(PostAction.UPDATE_POST)
        assertThat(actionCaptor.firstValue.payload).isEqualTo(postModel)
        assertThat(viewModel.mediaMarkedUploadingOnStartIds).isEqualTo(mediaIDs)
        verify(postRepository).saveDbSnapshot()
    }

    @Test
    fun `saves post to DB and does not update media IDs for non-Aztec editor`() {
        whenever(postRepository.postHasChangesFromDb()).thenReturn(true)

        viewModel.savePostToDb(context, postRepository, false)

        verify(dispatcher).dispatch(actionCaptor.capture())
        assertThat(actionCaptor.firstValue.type).isEqualTo(PostAction.UPDATE_POST)
        assertThat(actionCaptor.firstValue.payload).isEqualTo(postModel)
        assertThat(viewModel.mediaMarkedUploadingOnStartIds).isEmpty()
        verify(postRepository).saveDbSnapshot()
    }

    @Test
    fun `does not save the post with no change`() {
        whenever(postRepository.postHasChangesFromDb()).thenReturn(false)

        viewModel.savePostToDb(context, postRepository, false)

        verify(dispatcher, never()).dispatch(any())
        verify(postRepository, never()).saveDbSnapshot()
    }

    @Test
    fun `isCurrentMediaMarkedUploadingDifferentToOriginal is false on non-Aztec editor`() {
        val result = viewModel.isCurrentMediaMarkedUploadingDifferentToOriginal(context, false, "")

        assertThat(result).isFalse()
    }

    @Test
    fun `isCurrentMediaMarkedUploadingDifferentToOriginal is true when media IDs have changed`() {
        val currentList = listOf("A")
        val updatedList = listOf("B")
        viewModel.mediaMarkedUploadingOnStartIds = currentList
        whenever(
                aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        content
                )
        ).thenReturn(
                updatedList
        )

        val result = viewModel.isCurrentMediaMarkedUploadingDifferentToOriginal(
                context,
                true,
                content
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `isCurrentMediaMarkedUploadingDifferentToOriginal is false when media IDs have not changed`() {
        val currentList = listOf("A")
        viewModel.mediaMarkedUploadingOnStartIds = currentList
        whenever(
                aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        content
                )
        ).thenReturn(
                currentList
        )

        val result = viewModel.isCurrentMediaMarkedUploadingDifferentToOriginal(
                context,
                true,
                content
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `does not update post object with no change`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(false))
    }

    @Test
    fun `returns update error when post is missing`() {
        whenever(postRepository.hasPost()).thenReturn(false)

        val result = viewModel.updatePostObject(context, true, postRepository) { PostFields(title, content) }

        assertThat(result).isEqualTo(UpdateResult.Error)
    }

    @Test
    fun `returns update error when get content function returns null`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) {
            UpdateFromEditor.Failed(
                    RuntimeException("Not found")
            )
        }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Error)
    }

    @Test
    fun `updates post title and date locally changed when title has changed`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(updatedTitle, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        assertThat(postModel.title).isEqualTo(updatedTitle)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
    }

    @Test
    fun `updates post content and date locally changed when content has changed`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(title, updatedContent) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        assertThat(postModel.content).isEqualTo(updatedContent)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
    }

    @Test
    fun `updates post date when media inserted on creation`() {
        viewModel.mediaInsertedOnCreation = true
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
        assertThat(viewModel.mediaInsertedOnCreation).isFalse()
    }

    @Test
    fun `updates post date when media list has changed`() {
        viewModel.mediaMarkedUploadingOnStartIds = listOf("A")
        whenever(
                aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        content
                )
        ).thenReturn(listOf("B"))
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
    }

    @Test
    fun `updates post date when status has changed`() {
        whenever(postRepository.hasStatusChangedFromWhenEditorOpened(postStatus)).thenReturn(true)
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObject(context, true, postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(false))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
    }

    private fun setupCurrentTime() {
        val now = Calendar.getInstance()
        now.set(2019, 10, 10, 10, 10, 0)
        now.timeZone = TimeZone.getTimeZone("UTC")
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(now)
        whenever(dateTimeUtilsWrapper.iso8601FromCalendar(now)).thenReturn(currentTime)
    }
}
