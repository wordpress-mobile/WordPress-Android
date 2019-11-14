package org.wordpress.android.ui.posts

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Error
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val CHANGE_SAVE_DELAY = 500L
private const val MAX_UNSAVED_POSTS = 50

class EditPostViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val aztecEditorWrapper: AztecEditorWrapper,
    private val localeManagerWrapper: LocaleManagerWrapper
) : ScopedViewModel(mainDispatcher) {
    private var mDebounceCounter = 0
    var mediaInsertedOnCreation: Boolean = false
    var mediaMarkedUploadingOnStartIds: List<String> = listOf()
    private val _onSavePostTriggered = MutableLiveData<Event<Unit>>()
    val onSavePostTriggered: LiveData<Event<Unit>> = _onSavePostTriggered

    fun savePost() {
        launch {
            if (mDebounceCounter < MAX_UNSAVED_POSTS) {
                mDebounceCounter++
                delay(CHANGE_SAVE_DELAY)
            }
            mDebounceCounter = 0
            _onSavePostTriggered.value = Event(Unit)
        }
    }

    fun sortMediaMarkedUploadingOnStartIds() {
        mediaMarkedUploadingOnStartIds = mediaMarkedUploadingOnStartIds.sorted()
    }

    fun savePostToDb(
        context: Context,
        postRepository: EditPostRepository,
        showAztecEditor: Boolean
    ) {
        postRepository.updateInTransaction { postModel ->
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postModel))

            if (showAztecEditor) {
                // update the list of uploading ids
                mediaMarkedUploadingOnStartIds = aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        postModel.content
                )
            }
            false
        }
    }

    fun updatePostObject(
        context: Context,
        mShowAztecEditor: Boolean,
        mEditPostRepository: EditPostRepository,
        updatedTitle: String,
        getUpdatedContent: ((String) -> String?)
    ): UpdateResult {
        if (!mEditPostRepository.hasPost()) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.")
            return Error
        }
        return mEditPostRepository.updateInTransaction { postModel ->
            getUpdatedContent(postModel.content)?.let { updatedContent ->
                val postTitleOrContentChanged = updatePostContentNewEditor(
                        context,
                        mShowAztecEditor,
                        mEditPostRepository,
                        postModel,
                        updatedTitle,
                        updatedContent
                )

                // only makes sense to change the publish date and locally changed date if the Post was actually changed
                if (postTitleOrContentChanged) {
                    mEditPostRepository.updatePublishDateIfShouldBePublishedImmediately(postModel)
                    val timeInMillis = localeManagerWrapper.getCurrentCalendar().timeInMillis
                    postModel
                            .setDateLocallyChanged(
                                    DateTimeUtils.iso8601FromTimestamp(timeInMillis / 1000)
                            )
                }

                Success(postTitleOrContentChanged)
            } ?: Error
        }
    }

    /**
     * Updates post object with given title and content
     */
    private fun updatePostContentNewEditor(
        context: Context,
        showAztecEditor: Boolean,
        postRepository: EditPostRepository,
        editedPost: PostModel?,
        title: String,
        content: String
    ): Boolean {
        if (editedPost == null) {
            return false
        }
        val titleChanged = editedPost.title != title
        editedPost.setTitle(title)
        val contentChanged: Boolean
        when {
            mediaInsertedOnCreation -> {
                mediaInsertedOnCreation = false
                contentChanged = true
            }
            isCurrentMediaMarkedUploadingDifferentToOriginal(
                    context,
                    showAztecEditor,
                    editedPost.content
            ) -> contentChanged = true
            else -> contentChanged = editedPost.content.compareTo(content) != 0
        }
        if (contentChanged) {
            editedPost.setContent(content)
        }

        val statusChanged = postRepository.hasStatusChanged(editedPost.status)

        if (!editedPost.isLocalDraft && (titleChanged || contentChanged || statusChanged)) {
            editedPost.setIsLocallyChanged(true)
            val timeInMillis = localeManagerWrapper.getCurrentCalendar().timeInMillis
            editedPost
                    .setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(timeInMillis / 1000))
        }

        return titleChanged || contentChanged
    }

    /*
      * for as long as the user is in the Editor, we check whether there are any differences in media items
      * being uploaded since they opened the Editor for this Post. If some items have finished, the current list
      * won't be equal and thus we'll know we need to save the Post content as it's changed, given the local
      * URLs will have been replaced with the remote ones.
     */
    fun isCurrentMediaMarkedUploadingDifferentToOriginal(
        context: Context,
        showAztecEditor: Boolean,
        newContent: String
    ): Boolean {
        // this method makes use of AztecEditorFragment methods. Make sure to only run if Aztec is the current editor.
        if (!showAztecEditor) {
            return false
        }
        val currentUploadingMedia = aztecEditorWrapper.getMediaMarkedUploadingInPostContent(
                context,
                newContent
        )

        return mediaMarkedUploadingOnStartIds != currentUploadingMedia.sorted()
    }

    sealed class UpdateResult {
        object Error : UpdateResult()
        data class Success(val postTitleOrContentChanged: Boolean) : UpdateResult()
    }
}
