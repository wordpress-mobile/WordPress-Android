package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.SyncPublishingFeatureConfig
import javax.inject.Inject

class SyncPublishingFeatureUtils @Inject constructor(
    private val syncPublishingFeatureConfig: SyncPublishingFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
) {
    private fun isSyncPublishingEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                syncPublishingFeatureConfig.isEnabled()
    }

    /**
     * This helper function aids in post-conflict resolution. When attempting to edit a post,
     * sending the "if_not_modified_since" to the backend will trigger a 409 error if a newer version
     * has already been uploaded from another device. This functionality should be encapsulated
     * by the SYNC_PUBLISHING feature flag. The function is used to generate the final RemotePostPayload
     * that is sent to the backend through PostActionBuilder.newPushPostAction(). By setting the
     * isConflictResolution = true, "if_not_modified_since" is not sent to server and the post overwrites
     * the remote version.
     */
    fun getRemotePostPayloadForPush(payload: PostStore.RemotePostPayload): PostStore.RemotePostPayload {
        if (isSyncPublishingEnabled().not()) {
            payload.isConflictResolution = true
        }
        return payload
    }
}
