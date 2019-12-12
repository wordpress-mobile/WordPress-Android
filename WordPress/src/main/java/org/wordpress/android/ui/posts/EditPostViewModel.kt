package org.wordpress.android.ui.posts

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor.PostFields
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Error
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Success
import org.wordpress.android.ui.posts.editor.AztecEditorFragmentStaticWrapper
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val CHANGE_SAVE_DELAY = 500L
private const val MAX_UNSAVED_POSTS = 50

class EditPostViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val aztecEditorFragmentStaticWrapper: AztecEditorFragmentStaticWrapper,
    private val siteStore: SiteStore,
    private val uploadUtils: UploadUtilsWrapper,
    private val postUtils: PostUtilsWrapper,
    private val pendingDraftsNotificationsUtils: PendingDraftsNotificationsUtilsWrapper,
    private val uploadService: UploadServiceFacade,
    private val dateTimeUtils: DateTimeUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var debounceCounter = 0
    private var saveJob: Job? = null
    var mediaMarkedUploadingOnStartIds: List<String> = listOf()
    private val _onSavePostTriggered = MutableLiveData<Event<Unit>>()
    val onSavePostTriggered: LiveData<Event<Unit>> = _onSavePostTriggered
    private val _onFinish = MutableLiveData<Event<Unit>>()
    val onFinish: LiveData<Event<Unit>> = _onFinish

    fun savePostOnline(
        isFirstTimePublish: Boolean,
        context: Context,
        editPostRepository: EditPostRepository,
        showAztecEditor: Boolean,
        site: SiteModel
    ) {
        savePostToDb(context, editPostRepository, showAztecEditor, site)
        postUtils.trackSavePostAnalytics(
                editPostRepository.getPost(),
                siteStore.getSiteByLocalId(editPostRepository.localSiteId)
        )

        uploadService.uploadPost(context, editPostRepository.id, isFirstTimePublish)

        pendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
                context,
                editPostRepository.id
        )
    }

    fun savePostLocally(
        context: Context,
        editPostRepository: EditPostRepository,
        showAztecEditor: Boolean,
        site: SiteModel
    ) {
        Log.d("vojta", "savePostLocally")
        if (editPostRepository.postWasChangedInCurrentSession()) {
            Log.d("vojta", "Post has edits")
            savePostToDb(context, editPostRepository, showAztecEditor, site)

            // now set the pending notification alarm to be triggered in the next day, week, and month
            pendingDraftsNotificationsUtils
                    .scheduleNextNotifications(
                            context,
                            editPostRepository.id,
                            editPostRepository.dateLocallyChanged
                    )
        }
    }

    fun updateAndSavePostAsync(
        context: Context,
        showAztecEditor: Boolean,
        postRepository: EditPostRepository,
        site: SiteModel,
        getUpdatedTitleAndContent: ((currentContent: String) -> UpdateFromEditor),
        onSaveAction: (() -> Unit)
    ) {
        Log.d("vojta", "updateAndSavePostAsync")
        launch {
            val postUpdated = withContext(bgDispatcher) {
                Log.d("vojta", "Starting post update on the background")
                (updatePostObject(
                        context,
                        showAztecEditor,
                        postRepository,
                        getUpdatedTitleAndContent
                ) is Success)
                        .also { success ->
                            if (success) {
                                Log.d("vojta", "Is success so saving to the DB")
                                savePostToDb(context, postRepository, showAztecEditor, site)
                            }
                        }
            }
            if (postUpdated) {
                Log.d("vojta", "Save action invoked")
                onSaveAction()
            }
        }
    }

    fun savePostWithDelay() {
        Log.d("vojta", "Saving post with delay")
        saveJob?.cancel()
        saveJob = launch {
            if (debounceCounter < MAX_UNSAVED_POSTS) {
                debounceCounter++
                delay(CHANGE_SAVE_DELAY)
            }
            debounceCounter = 0
            _onSavePostTriggered.value = Event(Unit)
        }
    }

    fun sortMediaMarkedUploadingOnStartIds() {
        mediaMarkedUploadingOnStartIds = mediaMarkedUploadingOnStartIds.sorted()
    }

    fun savePostToDb(
        context: Context,
        postRepository: EditPostRepository,
        showAztecEditor: Boolean,
        site: SiteModel
    ) {
        Log.d("vojta", "Saving post to DB")
        if (postRepository.postHasChangesFromDb()) {
            val post = checkNotNull(postRepository.getEditablePost())
            // mark as pending if the user doesn't have publishing rights
            if (!uploadUtils.userCanPublish(site)) {
                when (postRepository.status) {
                    PostStatus.UNKNOWN,
                    PostStatus.PUBLISHED,
                    PostStatus.SCHEDULED,
                    PostStatus.PRIVATE ->
                        post.setStatus(PostStatus.PENDING.toString())
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.TRASHED -> {
                    }
                }
            }
            post.setIsLocallyChanged(true)
            post.setDateLocallyChanged(dateTimeUtils.currentTimeInIso8601UTC())
            Log.d("vojta", "Post has changes so really saving")
            postRepository.saveDbSnapshot()
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))

            if (showAztecEditor) {
                // update the list of uploading ids
                launch(bgDispatcher) {
                    mediaMarkedUploadingOnStartIds = aztecEditorFragmentStaticWrapper.getMediaMarkedUploadingInPostContent(
                            context,
                            postRepository.content
                    )
                }
            }
        }
    }

    fun updatePostObject(
        context: Context,
        showAztecEditor: Boolean,
        postRepository: EditPostRepository,
        getUpdatedTitleAndContent: ((currentContent: String) -> UpdateFromEditor)
    ): UpdateResult {
        Log.d("vojta", "updatePostObject")
        if (!postRepository.hasPost()) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.")
            return Error
        }
        return postRepository.updateInTransaction { postModel ->
            when (val updateFromEditor = getUpdatedTitleAndContent(postModel.content)) {
                is PostFields -> {
                    val postTitleOrContentChanged = updatePostContentNewEditor(
                            context,
                            showAztecEditor,
                            postRepository,
                            postModel,
                            updateFromEditor.title,
                            updateFromEditor.content
                    )

                    // only makes sense to change the publish date and locally changed date if the Post was actually changed
                    if (postTitleOrContentChanged) {
                        postRepository.updatePublishDateIfShouldBePublishedImmediately(
                                postModel
                        )
                        postModel.setDateLocallyChanged(dateTimeUtils.currentTimeInIso8601UTC())
                    }

                    Log.d("vojta", "updatePostObject: success - $postTitleOrContentChanged")
                    Success(postTitleOrContentChanged)
                }
                is UpdateFromEditor.Failed -> Error
            }
        }
    }

    fun updatePostAsync(
        context: Context,
        showAztecEditor: Boolean,
        postRepository: EditPostRepository,
        getUpdatedTitleAndContent: ((currentContent: String) -> UpdateFromEditor)
    ) {
        launch(bgDispatcher) {
            postRepository.updatePostAsync({ postModel ->
                when (val updateFromEditor = getUpdatedTitleAndContent(postModel.content)) {
                    is PostFields -> {
                        val postTitleOrContentChanged = updatePostContentNewEditor(
                                context,
                                showAztecEditor,
                                postRepository,
                                postModel,
                                updateFromEditor.title,
                                updateFromEditor.content
                        )

                        // only makes sense to change the publish date and locally changed date if the Post was actually changed
                        if (postTitleOrContentChanged) {
                            postRepository.updatePublishDateIfShouldBePublishedImmediately(
                                    postModel
                            )
                            postModel.setDateLocallyChanged(dateTimeUtils.currentTimeInIso8601UTC())
                        }
                        postTitleOrContentChanged
                    }
                    is UpdateFromEditor.Failed -> false
                }
            })
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
        Log.d("vojta", "updatePostContentNewEditor")
        if (editedPost == null) {
            return false
        }
        val titleChanged = editedPost.title != title
        editedPost.setTitle(title)
        val contentChanged: Boolean = when {
            isCurrentMediaMarkedUploadingDifferentToOriginal(
                    context,
                    showAztecEditor,
                    editedPost.content
            ) -> true
            else -> editedPost.content != content
        }
        if (contentChanged) {
            editedPost.setContent(content)
        }

        val statusChanged = postRepository.hasStatusChangedFromWhenEditorOpened(editedPost.status)

        if (!editedPost.isLocalDraft && (titleChanged || contentChanged || statusChanged)) {
            editedPost.setIsLocallyChanged(true)
            editedPost.setDateLocallyChanged(dateTimeUtils.currentTimeInIso8601UTC())
        }

        Log.d("vojta", "updatePostContentNewEditor - ${titleChanged || contentChanged}")
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
        val currentUploadingMedia = aztecEditorFragmentStaticWrapper.getMediaMarkedUploadingInPostContent(
                context,
                newContent
        )

        return mediaMarkedUploadingOnStartIds != currentUploadingMedia.sorted()
    }

    fun finish() {
        _onFinish.postValue(Event(Unit))
    }

    sealed class UpdateResult {
        object Error : UpdateResult()
        data class Success(val postTitleOrContentChanged: Boolean) : UpdateResult()
    }

    sealed class UpdateFromEditor {
        data class PostFields(val title: String, val content: String) : UpdateFromEditor()
        data class Failed(val exception: Exception) : UpdateFromEditor()
    }
}
