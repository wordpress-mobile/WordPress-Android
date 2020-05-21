package org.wordpress.android.ui.posts

import android.annotation.SuppressLint
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostModelUploadStatusTracker @Inject constructor(
    private val uploadStore: UploadStore,
    private val uploadActionUseCase: UploadActionUseCase
) {
    /*
    Using `SparseArray` is results in ArrayIndexOutOfBoundsException when we are trying to put a new item. Although
    the reason for the crash is unclear, this defeats the whole purpose of using a `SparseArray`. Furthermore,
    `SparseArray` is actually not objectively better than using a `HashMap` and in this case `HashMap` should perform
    better due to higher number of items.

    https://github.com/wordpress-mobile/WordPress-Android/issues/11487
     */
    @SuppressLint("UseSparseArrays")
    private val uploadStatusMap = HashMap<Int, PostListItemUploadStatus>()

    fun getUploadStatus(post: PostModel, siteModel: SiteModel): PostListItemUploadStatus {
        uploadStatusMap[post.id]?.let { return it }
        val uploadError = uploadStore.getUploadErrorForPost(post)
        val isUploadingOrQueued = UploadService.isPostUploadingOrQueued(post)
        val hasInProgressMediaUpload = UploadService.hasInProgressMediaUploadsForPost(post)
        val newStatus = PostListItemUploadStatus(
                uploadError = uploadError,
                mediaUploadProgress = (UploadService.getMediaUploadProgressForPost(post) * 100).roundToInt(),
                isUploading = UploadService.isPostUploading(post),
                isUploadingOrQueued = isUploadingOrQueued,
                isQueued = UploadService.isPostQueued(post),
                isUploadFailed = uploadStore.isFailedPost(post),
                hasInProgressMediaUpload = hasInProgressMediaUpload,
                hasPendingMediaUpload = UploadService.hasPendingMediaUploadsForPost(post),
                isEligibleForAutoUpload = uploadActionUseCase.isEligibleForAutoUpload(siteModel, post),
                uploadWillPushChanges = uploadActionUseCase.uploadWillPushChanges(post)
        )
        uploadStatusMap[post.id] = newStatus
        return newStatus
    }

    fun invalidateUploadStatus(localPostIds: List<Int>) {
        localPostIds.forEach { uploadStatusMap.remove(it) }
    }
}
