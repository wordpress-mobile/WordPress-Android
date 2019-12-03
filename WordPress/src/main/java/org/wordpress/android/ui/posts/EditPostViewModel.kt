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
import org.wordpress.android.fluxc.generated.UploadActionBuilder
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
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val aztecEditorFragmentStaticWrapper: AztecEditorFragmentStaticWrapper,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val siteStore: SiteStore,
    private val uploadUtils: UploadUtilsWrapper,
    private val postUtils: PostUtilsWrapper,
    private val pendingDraftsNotificationsUtils: PendingDraftsNotificationsUtilsWrapper,
    private val uploadService: UploadServiceFacade,
    private val dateTimeUtils: DateTimeUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var debounceCounter = 0
    private var saveJob: Job? = null
    var mediaInsertedOnCreation: Boolean = false
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
        site: SiteModel,
        doFinishActivity: Boolean
    ) {
        launch(bgDispatcher) {
            // mark as pending if the user doesn't have publishing rights
            if (!uploadUtils.userCanPublish(site)) {
                when (editPostRepository.status) {
                    PostStatus.UNKNOWN,
                    PostStatus.PUBLISHED,
                    PostStatus.SCHEDULED,
                    PostStatus.PRIVATE ->
                        editPostRepository.updateStatus(PostStatus.PENDING)
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.TRASHED -> {
                    }
                }
            }

            savePostToDb(context, editPostRepository, showAztecEditor)
            postUtils.trackSavePostAnalytics(
                    editPostRepository.getPost(),
                    siteStore.getSiteByLocalId(editPostRepository.localSiteId)
            )

            uploadService.uploadPost(context, editPostRepository.id, isFirstTimePublish)

            pendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
                    context,
                    editPostRepository.id
            )
            if (doFinishActivity) {
                withContext(mainDispatcher) {
                    _onFinish.value = Event(Unit)
                }
            }
        }
    }

    fun savePostLocally(
        context: Context,
        editPostRepository: EditPostRepository,
        showAztecEditor: Boolean,
        doFinishActivity: Boolean
    ) {
        launch(bgDispatcher) {
            Log.d("vojta", "savePostLocally")
            if (editPostRepository.postHasEdits()) {
                Log.d("vojta", "Post has edits")
                editPostRepository.updateInTransaction { postModel ->
                    Log.d("vojta", "Updating post in transaction")
                    // Changes have been made - save the post and ask for the post list to refresh
                    // We consider this being "manual save", it will replace some Android "spans" by an html
                    // or a shortcode replacement (for instance for images and galleries)

                    // Update the post object directly, without re-fetching the fields from the EditorFragment
                    updatePostLocallyChangedOnStatusOrMediaChange(
                            context,
                            postModel,
                            showAztecEditor,
                            editPostRepository
                    )
                    true
                }
                savePostToDb(context, editPostRepository, showAztecEditor)

                // For self-hosted sites, when exiting the editor without uploading, the `PostUploadModel
                // .uploadState`
                // can get stuck in `PENDING`. This happens in this scenario:
                //
                // 1. The user edits an existing post
                // 2. Adds an image -- this creates the `PostUploadModel` as `PENDING`
                // 3. Exits the editor by tapping on Back (not saving or publishing)
                //
                // If the `uploadState` is stuck at `PENDING`, the Post List will indefinitely show a “Queued post”
                // label.
                //
                // The `uploadState` does not get stuck on `PENDING` for WPCom because the app will automatically
                // start a remote auto-save when the editor exits. Hence, the `PostUploadModel` eventually gets
                // updated.
                //
                // Marking the `PostUploadModel` as `CANCELLED` when exiting should be fine for all site types since
                // we do not currently have any special handling for cancelled uploads. Eventually, the user will
                // restart them and the `uploadState` will be corrected.
                //
                // See `PostListUploadStatusTracker` and `PostListItemUiStateHelper.createUploadUiState` for how
                // the Post List determines what label to use.
                dispatcher.dispatch(UploadActionBuilder.newCancelPostAction(editPostRepository.getEditablePost()))

                // now set the pending notification alarm to be triggered in the next day, week, and month
                pendingDraftsNotificationsUtils
                        .scheduleNextNotifications(
                                context,
                                editPostRepository.id,
                                editPostRepository.dateLocallyChanged
                        )
            }
            if (doFinishActivity) {
                withContext(mainDispatcher) {
                    Log.d("vojta", "Save action invoked")
                    _onFinish.value = Event(Unit)
                }
            }
        }
    }

    private fun updatePostLocallyChangedOnStatusOrMediaChange(
        context: Context,
        editedPost: PostModel?,
        showAztecEditor: Boolean,
        editPostRepository: EditPostRepository
    ) {
        Log.d("vojta", "updatePostLocallyChangedOnStatusOrMediaChange")
        if (editedPost == null) {
            Log.d("vojta", "post is null")
            return
        }
        val contentChanged: Boolean
        if (mediaInsertedOnCreation) {
            mediaInsertedOnCreation = false
            contentChanged = true
        } else {
            contentChanged = isCurrentMediaMarkedUploadingDifferentToOriginal(
                    context,
                    showAztecEditor,
                    editedPost.content
            )
        }
        val statusChanged: Boolean = editPostRepository.hasStatusChangedFromWhenEditorOpened(
                editedPost.status
        )
        if (!editedPost.isLocalDraft && (contentChanged || statusChanged)) {
            Log.d(
                    "vojta",
                    "really updating post: ${!editedPost.isLocalDraft && (contentChanged || statusChanged)}"
            )
            editedPost.setIsLocallyChanged(true)
            val currentTime = localeManagerWrapper.getCurrentCalendar()
            editedPost.setDateLocallyChanged(dateTimeUtils.iso8601UTCFromCalendar(currentTime))
        }
    }

    fun updateAndSavePostAsync(
        context: Context,
        showAztecEditor: Boolean,
        postRepository: EditPostRepository,
        getUpdatedTitleAndContent: ((currentContent: String) -> UpdateFromEditor),
        onSaveAction: (() -> Unit)? = null
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
                                savePostToDb(context, postRepository, showAztecEditor)
                            }
                        }
            }
            if (postUpdated) {
                Log.d("vojta", "Save action invoked")
                onSaveAction?.invoke()
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
        showAztecEditor: Boolean
    ) {
        Log.d("vojta", "Saving post to DB")
        if (postRepository.postHasChangesFromDb()) {
            Log.d("vojta", "Post has changes so really saving")
            postRepository.saveDbSnapshot()
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postRepository.getEditablePost()))

            if (showAztecEditor) {
                // update the list of uploading ids
                mediaMarkedUploadingOnStartIds = aztecEditorFragmentStaticWrapper.getMediaMarkedUploadingInPostContent(
                        context,
                        postRepository.content
                )
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
                        val currentTime = localeManagerWrapper.getCurrentCalendar()
                        postModel
                                .setDateLocallyChanged(
                                        dateTimeUtils.iso8601UTCFromCalendar(currentTime)
                                )
                    }

                    Log.d("vojta", "updatePostObject: success - $postTitleOrContentChanged")
                    Success(postTitleOrContentChanged)
                }
                is UpdateFromEditor.Failed -> Error
            }
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
            else -> contentChanged = editedPost.content != content
        }
        if (contentChanged) {
            editedPost.setContent(content)
        }

        val statusChanged = postRepository.hasStatusChangedFromWhenEditorOpened(editedPost.status)

        if (!editedPost.isLocalDraft && (titleChanged || contentChanged || statusChanged)) {
            editedPost.setIsLocallyChanged(true)
            val currentTime = localeManagerWrapper.getCurrentCalendar()
            editedPost
                    .setDateLocallyChanged(dateTimeUtils.iso8601UTCFromCalendar(currentTime))
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

    sealed class UpdateResult {
        object Error : UpdateResult()
        data class Success(val postTitleOrContentChanged: Boolean) : UpdateResult()
    }

    sealed class UpdateFromEditor {
        data class PostFields(val title: String, val content: String) : UpdateFromEditor()
        data class Failed(val exception: Exception) : UpdateFromEditor()
    }
}
