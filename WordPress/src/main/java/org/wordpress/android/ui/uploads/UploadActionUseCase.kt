package org.wordpress.android.ui.uploads

import dagger.Reusable
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.DO_NOTHING
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.REMOTE_AUTO_SAVE
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD_AS_DRAFT
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

const val MAXIMUM_AUTO_UPLOAD_RETRIES = 3
private const val TWO_DAYS_IN_MILLIS = 1000 * 60 * 60 * 24 * 2

@Reusable
class UploadActionUseCase @Inject constructor(
    private val uploadStore: UploadStore,
    private val postUtilsWrapper: PostUtilsWrapper,
    private val uploadServiceFacade: UploadServiceFacade
) {
    enum class UploadAction {
        REMOTE_AUTO_SAVE, UPLOAD_AS_DRAFT, UPLOAD, DO_NOTHING
    }

    fun getAutoUploadAction(post: PostImmutableModel, site: SiteModel): UploadAction {
        val twoDaysAgoTimestamp = Date().time - TWO_DAYS_IN_MILLIS
        // Don't auto-upload/save changes which are older than 2 days
        if (DateTimeUtils.timestampFromIso8601Millis(post.dateLocallyChanged) < twoDaysAgoTimestamp) {
            return DO_NOTHING
        }

        // Do not auto-upload empty post
        if (!postUtilsWrapper.isPublishable(post)) {
            return DO_NOTHING
        }

        // Do not auto-upload post which is in conflict with remote
        if (postUtilsWrapper.isPostInConflictWithRemote(post)) {
            return DO_NOTHING
        }

        // Do not auto-upload post which we already tried to upload certain number of times
        if (uploadStore.getNumberOfPostAutoUploadAttempts(post) >= MAXIMUM_AUTO_UPLOAD_RETRIES) {
            return DO_NOTHING
        }

        // Do not auto-upload post which is currently being uploaded
        if (uploadServiceFacade.isPostUploadingOrQueued(post)) {
            return DO_NOTHING
        }

        val action = getUploadAction(post)
        // Don't remoteAutoSave changes which were already remoteAutoSaved or when on a self-hosted site
        if (action == REMOTE_AUTO_SAVE &&
            (UploadUtils.postLocalChangesAlreadyRemoteAutoSaved(post) || !site.isUsingWpComRestApi)
        ) {
            return DO_NOTHING
        }

        // Do not auto-upload post which is being edited
        if (postUtilsWrapper.isPostCurrentlyBeingEdited(post)) {
            return DO_NOTHING
        }

        return action
    }

    fun getUploadAction(post: PostImmutableModel): UploadAction {
        return when {
            uploadWillPushChanges(post) ->
                // We are sure we can push the post as the user has explicitly confirmed the changes
                UPLOAD
            post.isLocalDraft ->
                // Local draft can always be uploaded as DRAFT as it doesn't exist on the server yet
                UPLOAD_AS_DRAFT
            !post.isLocallyChanged ->
                // the post isn't local draft and isn't locally changed -> there is nothing new to upload
                DO_NOTHING
            else -> REMOTE_AUTO_SAVE
        }
    }

    fun isEligibleForAutoUpload(site: SiteModel, post: PostImmutableModel): Boolean {
        return when (getAutoUploadAction(post, site)) {
            UPLOAD -> true
            UPLOAD_AS_DRAFT, REMOTE_AUTO_SAVE, DO_NOTHING -> false
        }
    }

    fun uploadWillPushChanges(post: PostImmutableModel) =
        post.changesConfirmedContentHashcode == post.contentHashcode()
}
