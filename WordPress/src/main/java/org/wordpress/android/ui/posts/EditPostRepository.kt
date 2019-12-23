package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.fromPost
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Arrays
import javax.inject.Inject

class EditPostRepository
@Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val postStore: PostStore,
    private val postUtils: PostUtilsWrapper
) {
    private var post: PostModel? = null
    private var postForUndo: PostModel? = null
    private var postSnapshotWhenEditorOpened: PostModel? = null
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

    private var locked = false

    fun update(action: (PostModel) -> Boolean): Boolean {
        reportTransactionState(true)
        val result = action(post!!)
        reportTransactionState(false)
        return result
    }

    fun replace(action: (PostModel) -> PostModel) {
        reportTransactionState(true)
        this.post = action(post!!)
        reportTransactionState(false)
    }

    fun set(action: () -> PostModel) {
        reportTransactionState(true)
        this.post = action()
        reportTransactionState(false)
    }

    @Synchronized
    private fun reportTransactionState(lock: Boolean) {
        if (lock && locked) {
            val message = "EditPostRepository: Transaction is writing on a locked thread ${Arrays.toString(
                    Thread.currentThread().stackTrace
            )}"
            AppLog.e(T.EDITOR, message)
            CrashLoggingUtils.log(message)
        }
        locked = lock
    }

    fun hasLocation() = post!!.hasLocation()

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

    fun saveSnapshot() {
        postSnapshotWhenEditorOpened = post?.clone()
    }

    fun isSnapshotDifferent(): Boolean =
            postSnapshotWhenEditorOpened == null || post != postSnapshotWhenEditorOpened

    fun hasSnapshot() = postSnapshotWhenEditorOpened != null

    fun updateStatusFromSnapshot(post: PostModel) {
        // the user has just tapped on "PUBLISH" on an empty post, make sure to set the status back to the
        // original post's status as we could not proceed with the action
        reportTransactionState(true)
        post.setStatus(postSnapshotWhenEditorOpened?.status ?: DRAFT.toString())
        reportTransactionState(false)
    }

    fun hasStatusChanged(postStatus: String?): Boolean {
        return postSnapshotWhenEditorOpened?.status != null && postStatus != postSnapshotWhenEditorOpened?.status
    }

    fun postHasEdits() = postUtils.postHasEdits(postSnapshotWhenEditorOpened, post!!)

    fun updateStatus(status: PostStatus) {
        update {
            it.setStatus(status.toString())
            true
        }
    }

    fun loadPostByLocalPostId(postId: Int) {
        reportTransactionState(true)
        post = postStore.getPostByLocalPostId(postId)
        reportTransactionState(false)
    }

    fun loadPostByRemotePostId(remotePostId: Long, site: SiteModel) {
        reportTransactionState(true)
        post = postStore.getPostByRemotePostId(remotePostId, site)
        reportTransactionState(false)
    }
}
