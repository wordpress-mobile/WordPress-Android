package org.wordpress.android.ui.posts

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor.Failed
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor.PostFields
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Error
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult.Success
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
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
    private val siteStore: SiteStore,
    private val uploadUtils: UploadUtilsWrapper,
    private val postUtils: PostUtilsWrapper,
    private val pendingDraftsNotificationsUtils: PendingDraftsNotificationsUtilsWrapper,
    private val uploadService: UploadServiceFacade,
    private val dateTimeUtils: DateTimeUtilsWrapper,
    private val networkUtils: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var debounceCounter = 0
    private var saveJob: Job? = null
    private val _onSavePostTriggered = MutableLiveData<Event<Unit>>()
    val onSavePostTriggered: LiveData<Event<Unit>> = _onSavePostTriggered
    private val _onFinish = MutableLiveData<Event<Boolean>>()
    val onFinish: LiveData<Event<Boolean>> = _onFinish

    fun savePostOnline(
        isFirstTimePublish: Boolean,
        context: Context,
        editPostRepository: EditPostRepository,
        site: SiteModel
    ): Boolean {
        savePostToDb(context, editPostRepository, site)
        return if (networkUtils.isNetworkAvailable()) {
            postUtils.trackSavePostAnalytics(
                    editPostRepository.getPost(),
                    siteStore.getSiteByLocalId(editPostRepository.localSiteId)
            )
            uploadService.uploadPost(context, editPostRepository.id, isFirstTimePublish)
            true
        } else {
            false
        }
    }

    fun retryUpload(
        isFirstTimePublish: Boolean,
        context: Context,
        editPostRepository: EditPostRepository
    ): Boolean {
        return if (networkUtils.isNetworkAvailable()) {
            uploadService.uploadPost(context, editPostRepository.id, isFirstTimePublish)
            true
        } else {
            false
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
    fun savePostToDb(
        context: Context,
        postRepository: EditPostRepository,
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
            handlePendingDraftNotifications(context, postRepository)
            Log.d("vojta", "Post has changes so really saving")
            postRepository.saveDbSnapshot()
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))
        }
    }

    private fun handlePendingDraftNotifications(
        context: Context,
        editPostRepository: EditPostRepository
    ) {
        if (editPostRepository.status == DRAFT) {
            // now set the pending notification alarm to be triggered in the next day, week, and month
            pendingDraftsNotificationsUtils
                    .scheduleNextNotifications(
                            context,
                            editPostRepository.id,
                            editPostRepository.dateLocallyChanged
                    )
        } else {
            pendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
                    context,
                    editPostRepository.id
            )
        }
    }

    fun updatePostObjectWithUI(
        postRepository: EditPostRepository,
        getUpdatedTitleAndContent: (currentContent: String) -> UpdateFromEditor
    ): UpdateResult {
        return postRepository.updateInTransaction { postModel ->
            updatePostObjectWithUI(getUpdatedTitleAndContent, postModel, postRepository)
        }
    }

    fun updatePostObjectWithUIAsync(
        postRepository: EditPostRepository,
        getUpdatedTitleAndContent: (currentContent: String) -> UpdateFromEditor,
        onSuccess: ((PostImmutableModel) -> Unit)? = null
    ) {
        postRepository.updatePostInTransactionAsync({ postModel ->
            val updateResult = updatePostObjectWithUI(getUpdatedTitleAndContent, postModel, postRepository)
            updateResult is Success
        }, onSuccess)
    }

    private fun updatePostObjectWithUI(
        getUpdatedTitleAndContent: (currentContent: String) -> UpdateFromEditor,
        postModel: PostModel,
        postRepository: EditPostRepository
    ): UpdateResult {
        Log.d("vojta", "updatePostObjectWithUI")
        if (!postRepository.hasPost()) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.")
            return Error
        }
        return when (val updateFromEditor = getUpdatedTitleAndContent(postModel.content)) {
            is PostFields -> {
                val postTitleOrContentChanged = updatePostContentNewEditor(
                        postModel,
                        updateFromEditor.title,
                        updateFromEditor.content
                )

                // only makes sense to change the publish date and locally changed date if the Post was actually changed
                if (postTitleOrContentChanged) {
                    postRepository.updatePublishDateIfShouldBePublishedImmediately(
                            postModel
                    )
                }

                Log.d("vojta", "updatePostObjectWithUI: success - $postTitleOrContentChanged")
                Success(postTitleOrContentChanged)
            }
            is Failed -> Error
        }
    }

    /**
     * Updates post object with given title and content
     */
    private fun updatePostContentNewEditor(
        editedPost: PostModel,
        title: String,
        content: String
    ): Boolean {
        Log.d("vojta", "updatePostContentNewEditor")
        val titleChanged = editedPost.title != title
        if (titleChanged) {
            editedPost.setTitle(title)
        }
        val contentChanged: Boolean = editedPost.content != content
        if (contentChanged) {
            editedPost.setContent(content)
        }

        Log.d("vojta", "updatePostContentNewEditor - ${titleChanged || contentChanged}")
        return titleChanged || contentChanged
    }

    fun finish(savedOnline: Boolean) {
        _onFinish.postValue(Event(savedOnline))
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
