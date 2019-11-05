package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
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
    var postSnapshotWhenEditorOpened: PostModel? = null
        get() = lock.read { field }
    var id: Int
        get() = readFromPost { this.id }
        set(value) {
            writeToPost { this.id = value }
        }
    var localSiteId: Int
        get() = readFromPost { this.localSiteId }
        set(value) {
            writeToPost { this.localSiteId = value }
        }
    var remotePostId: Long
        get() = readFromPost { this.remotePostId }
        set(value) {
            writeToPost { this.remotePostId = value }
        }
    var title: String
        get() = readFromPost { this.title }
        set(value) {
            writeToPost { this.title = value }
        }
    var autoSaveTitle: String
        get() = readFromPost { this.autoSaveTitle }
        set(value) {
            writeToPost { this.autoSaveTitle = value }
        }
    var content: String
        get() = readFromPost { this.content }
        set(value) {
            writeToPost { this.content = value }
        }
    var autoSaveContent: String
        get() = readFromPost { this.autoSaveContent }
        set(value) {
            writeToPost { this.autoSaveContent = value }
        }
    var excerpt: String
        get() = readFromPost { this.excerpt }
        set(value) {
            writeToPost { this.excerpt = value }
        }
    var autoSaveExcerpt: String
        get() = readFromPost { this.autoSaveExcerpt }
        set(value) {
            writeToPost { this.autoSaveExcerpt = value }
        }
    var password: String
        get() = readFromPost { this.password }
        set(value) {
            writeToPost { this.password = value }
        }
    var status: PostStatus
        get() = readFromPost { PostStatus.fromPost(this) }
        set(value) {
            writeToPost { this.status = value.toString() }
        }
    var isPage: Boolean
        get() = readFromPost { this.isPage }
        set(value) {
            writeToPost { this.setIsPage(value) }
        }
    var isLocalDraft: Boolean
        get() = readFromPost { this.isLocalDraft }
        set(value) {
            writeToPost { this.setIsLocalDraft(value) }
        }
    var isLocallyChanged: Boolean
        get() = readFromPost { this.isLocallyChanged }
        set(value) {
            writeToPost { this.setIsLocallyChanged(value) }
        }
    var dateLocallyChanged: String
        get() = readFromPost { this.dateLocallyChanged }
        set(value) {
            writeToPost { this.dateLocallyChanged = value }
        }
    var featuredImageId: Long
        get() = readFromPost { this.featuredImageId }
        set(value) {
            writeToPost { this.featuredImageId = value }
        }
    var dateCreated: String
        get() = readFromPost { this.dateCreated }
        set(value) {
            writeToPost { this.dateCreated = value }
        }
    var changesConfirmedContentHashcode: Int
        get() = readFromPost { this.changesConfirmedContentHashcode }
        set(value) {
            writeToPost { this.changesConfirmedContentHashcode = value }
        }
    var postFormat: String
        get() = readFromPost { this.postFormat }
        set(value) {
            writeToPost { this.postFormat = value }
        }
    var slug: String
        get() = readFromPost { this.slug }
        set(value) {
            writeToPost { this.slug = value }
        }
    var link: String
        get() = readFromPost { this.link }
        set(value) {
            writeToPost { this.link = value }
        }
    var location: PostLocation
        get() = readFromPost { this.location }
        set(value) {
            writeToPost { this.location = value }
        }
    var tagNameList: List<String>
        get() = readFromPost { this.tagNameList }
        set(value) {
            writeToPost { this.tagNameList = value }
        }
    var categoryIdList: List<Long>
        get() = readFromPost { this.categoryIdList }
        set(value) {
            writeToPost { this.categoryIdList = value }
        }
    private val snapshotStatus: PostStatus?
        get() = lock.read {
            postSnapshotWhenEditorOpened?.let { PostStatus.fromPost(it) }
        }

    private val lock = ReentrantReadWriteLock()

    fun clearLocation() = writeToPost { this.clearLocation() }
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

    fun updatePublishDateIfShouldBePublishedImmediately() {
        if (postUtils.shouldPublishImmediately(status, dateCreated)) {
            dateCreated = DateTimeUtils.iso8601FromDate(localeManagerWrapper.getCurrentCalendar().time)
        }
    }

    fun updatePostTitleIfDifferent(newTitle: String): Boolean {
        return if (title != newTitle) {
            title = newTitle
            true
        } else {
            false
        }
    }

    fun isPublishable() = post?.let { postUtils.isPublishable(it) } ?: false

    fun contentHashcode() = readFromPost { this.contentHashcode() }

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

    fun updateStatusFromSnapshot() {
        // the user has just tapped on "PUBLISH" on an empty post, make sure to set the status back to the
        // original post's status as we could not proceed with the action
        status = snapshotStatus ?: DRAFT
    }

    fun hasStatusChanged(): Boolean {
        return postSnapshotWhenEditorOpened?.status != null && post?.status != postSnapshotWhenEditorOpened?.status
    }

    fun postHasEdits() = postUtils.postHasEdits(postSnapshotWhenEditorOpened, post)

    private fun <Y> writeToPost(action: PostModel.() -> Y) = lock.write { post!!.action() }

    private fun <Y> readFromPost(action: PostModel.() -> Y) = lock.read { post!!.action() }
}
