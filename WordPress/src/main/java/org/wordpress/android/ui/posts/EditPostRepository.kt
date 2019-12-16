package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.fromPost
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import javax.inject.Named
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

class EditPostRepository
@Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val postStore: PostStore,
    private val postUtils: PostUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private var post: PostModel? = null
    private var postForUndo: PostModel? = null
    private var postSnapshotWhenEditorOpened: PostModel? = null
    private var postSnapshotForDb: PostModel? = null
    val id: Int
        get() = post!!.id
    val localSiteId: Int
        get() = post!!.localSiteId
    val remotePostId: Long
        get() = post!!.remotePostId
    val title: String
        get() = post!!.title
    val autoSaveTitle: String?
        get() = post!!.autoSaveTitle
    val content: String
        get() = post!!.content
    val autoSaveContent: String?
        get() = post!!.autoSaveContent
    val excerpt: String
        get() = post!!.excerpt
    val autoSaveExcerpt: String?
        get() = post!!.autoSaveExcerpt
    val password: String
        get() = post!!.password
    val status: PostStatus
        get() = fromPost(getPost())
    val isPage: Boolean
        get() = post!!.isPage
    val isLocalDraft: Boolean
        get() = post!!.isLocalDraft
    val isLocallyChanged: Boolean
        get() = post!!.isLocallyChanged
    val featuredImageId: Long
        get() = post!!.featuredImageId
    val dateCreated: String
        get() = post!!.dateCreated
    val changesConfirmedContentHashcode: Int
        get() = post!!.changesConfirmedContentHashcode
    val postFormat: String
        get() = post!!.postFormat
    val slug: String
        get() = post!!.slug
    val link: String
        get() = post!!.link
    val location: PostLocation
        get() = post!!.location
    val tagNameList: List<String>
        get() = post!!.tagNameList
    val dateLocallyChanged: String
        get() = post!!.dateLocallyChanged

    private val lock = ReentrantReadWriteLock()

    private val _postChanged = MutableLiveData<Event<PostImmutableModel>>()
    val postChanged: LiveData<Event<PostImmutableModel>> = _postChanged

    fun <T> updateInTransaction(action: (PostModel) -> T): T {
        val result = lock.write {
            action(requireNotNull(post))
        }
        runBlocking {
            launch(mainDispatcher) {
                _postChanged.value = Event(requireNotNull(post))
            }
        }
        return result
    }

    fun updatePostInTransactionAsync(
        action: (PostModel) -> Boolean,
        onSuccess: ((PostImmutableModel) -> Unit)? = null
    ) {
        launch {
            val isUpdated = withContext(bgDispatcher) {
                lock.write {
                    action(requireNotNull(post))
                }
            }
            if (isUpdated) {
                requireNotNull(post).let {
                    _postChanged.value = Event(it)
                    onSuccess?.invoke(it)
                }
            }
        }
    }

    fun replaceInTransaction(action: (PostModel) -> PostModel) = lock.write {
        this.post = action(requireNotNull(post))
    }

    fun setInTransaction(action: () -> PostModel) = lock.write {
        this.post = action()
    }

    fun hasLocation() = requireNotNull(post).hasLocation()

    fun hasPost() = post != null
    fun getPost(): PostImmutableModel? = post
    fun getEditablePost() = post

    fun getPostForUndo() = postForUndo

    fun hasStatus(status: PostStatus): Boolean {
        return post?.status == status.toString()
    }

    fun getPendingMediaForPost(): Set<MediaModel> =
            UploadService.getPendingMediaForPost(post)

    fun getPendingOrInProgressMediaUploadsForPost(): List<MediaModel> =
            UploadService.getPendingOrInProgressMediaUploadsForPost(post)

    fun updatePublishDateIfShouldBePublishedImmediately(post: PostModel) {
        if (postUtils.shouldPublishImmediately(fromPost(post), post.dateCreated)) {
            post.setDateCreated(DateTimeUtils.iso8601FromDate(localeManagerWrapper.getCurrentCalendar().time))
        }
    }

    fun isPostPublishable() = post?.let { postUtils.isPublishable(it) } ?: false

    fun saveForUndo() {
        postForUndo = post?.clone()
    }

    fun undo() {
        this.post = postForUndo?.clone()
    }

    fun postHasChangesFromDb(): Boolean {
        checkNotNull(postSnapshotForDb) { "Post snapshot from DB cannot be null at this point" }
        return post != postSnapshotForDb ||
                post?.changesConfirmedContentHashcode != postSnapshotForDb?.changesConfirmedContentHashcode
    }

    fun saveDbSnapshot() {
        postSnapshotForDb = post?.clone()
    }

    fun savePostSnapshotWhenEditorOpened() {
        postSnapshotWhenEditorOpened = post?.clone()
    }

    fun hasPostSnapshotWhenEditorOpened() = postSnapshotWhenEditorOpened != null

    fun updateStatusFromPostSnapshotWhenEditorOpened(post: PostModel) {
        // the user has just tapped on "PUBLISH" on an empty post, make sure to set the status back to the
        // original post's status as we could not proceed with the action
        post.setStatus(postSnapshotWhenEditorOpened?.status ?: DRAFT.toString())
    }

    fun postWasChangedInCurrentSession() = postUtils.postHasEdits(postSnapshotWhenEditorOpened, requireNotNull(post)!!)

    fun setStatus(status: PostStatus) {
        checkNotNull(post, { "Post cannot be null when setting status" }).let {
            val updatedPost = status.toString()
            if (it.status != updatedPost) {
                it.setStatus(updatedPost)
            }
        }
    }

    fun loadPostByLocalPostId(postId: Int) {
        lock.write {
            post = postStore.getPostByLocalPostId(postId)
            saveDbSnapshot()
        }
    }

    fun loadPostByRemotePostId(remotePostId: Long, site: SiteModel) {
        lock.write {
            post = postStore.getPostByRemotePostId(remotePostId, site)
            saveDbSnapshot()
        }
    }
}
