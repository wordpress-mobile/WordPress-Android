package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.fromPost
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import kotlin.concurrent.read
import kotlin.concurrent.write

class EditPostRepository
@Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val postUtils: PostUtilsWrapper
) {
    private var post: PostModel? = null
    var postForUndo: PostModel? = null
        get() = lock.read { field }
        set(value) {
            lock.write {
                field = value
            }
        }
    private var postSnapshotWhenEditorOpened: PostModel? = null
    val id: Int
        get() = readFromPost { this.id }
    val localSiteId: Int
        get() = readFromPost { this.localSiteId }
    val remotePostId: Long
        get() = readFromPost { this.remotePostId }
    val title: String
        get() = readFromPost { this.title }
    val autoSaveTitle: String
        get() = readFromPost { this.autoSaveTitle }
    val content: String
        get() = readFromPost { this.content }
    val autoSaveContent: String
        get() = readFromPost { this.autoSaveContent }
    val excerpt: String
        get() = readFromPost { this.excerpt }
    val autoSaveExcerpt: String
        get() = readFromPost { this.autoSaveExcerpt }
    val password: String
        get() = readFromPost { this.password }
    val status: PostStatus
        get() = readFromPost { fromPost(this) }
    val isPage: Boolean
        get() = readFromPost { this.isPage }
    val isLocalDraft: Boolean
        get() = readFromPost { this.isLocalDraft }
    val isLocallyChanged: Boolean
        get() = readFromPost { this.isLocallyChanged }
    val featuredImageId: Long
        get() = readFromPost { this.featuredImageId }
    val dateCreated: String
        get() = readFromPost { this.dateCreated }
    val changesConfirmedContentHashcode: Int
        get() = readFromPost { this.changesConfirmedContentHashcode }
    val postFormat: String
        get() = readFromPost { this.postFormat }
    val slug: String
        get() = readFromPost { this.slug }
    val link: String
        get() = readFromPost { this.link }
    val location: PostLocation
        get() = readFromPost { this.location }
    val tagNameList: List<String>
        get() = readFromPost { this.tagNameList }

    private val lock = ReentrantReadWriteLock()

    fun updateInTransaction(action: (PostModel) -> Boolean) = lock.write {
        post?.let {
            action(it)
        } ?: false
    }

    fun hasLocation() = readFromPost { this.hasLocation() }

    fun hasPost() = post != null
    fun getPost() = lock.read { post }
    fun setPost(post: PostModel) {
        lock.write {
            this.post = post
        }
    }

    fun hasStatus(status: PostStatus): Boolean {
        return post?.status == status.toString()
    }

    fun getPendingMediaForPost(): Set<MediaModel> = readFromPost {
        UploadService.getPendingMediaForPost(this)
    }

    fun getPendingOrInProgressMediaUploadsForPost(): List<MediaModel> = readFromPost {
        UploadService.getPendingOrInProgressMediaUploadsForPost(this)
    }

    fun updatePublishDateIfShouldBePublishedImmediately(post: PostModel) {
        if (postUtils.shouldPublishImmediately(fromPost(post), post.dateCreated)) {
            post.dateCreated = DateTimeUtils.iso8601FromDate(localeManagerWrapper.getCurrentCalendar().time)
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
            lock.read { postSnapshotWhenEditorOpened == null || post != postSnapshotWhenEditorOpened }

    fun hasSnapshot() = lock.read { postSnapshotWhenEditorOpened != null }

    fun updateStatusFromSnapshot(post: PostModel) {
        // the user has just tapped on "PUBLISH" on an empty post, make sure to set the status back to the
        // original post's status as we could not proceed with the action
        post.status = postSnapshotWhenEditorOpened?.status ?: DRAFT.toString()
    }

    fun hasStatusChanged(postStatus: String?): Boolean {
        return postSnapshotWhenEditorOpened?.status != null && postStatus != postSnapshotWhenEditorOpened?.status
    }

    fun postHasEdits() = postUtils.postHasEdits(postSnapshotWhenEditorOpened, post)

    private fun <Y> readFromPost(action: PostModel.() -> Y) = lock.read { post!!.action() }
    fun updateStatus(status: PostStatus) {
        updateInTransaction {
            it.status = status.toString()
            true
        }
    }
}
