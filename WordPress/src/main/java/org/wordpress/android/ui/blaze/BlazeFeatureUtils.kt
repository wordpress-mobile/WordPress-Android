package org.wordpress.android.ui.blaze

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.BlazeFeatureConfig
import javax.inject.Inject

class BlazeFeatureUtils @Inject constructor(
    private val blazeFeatureConfig: BlazeFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper
) {

    fun shouldShowPromoteWithBlaze(
        postStatus: PostStatus,
        siteModel: SiteModel,
        postModel: PostModel
    ): Boolean {
        //add the logic to check whether the site is eligible for blaze
        return !buildConfigWrapper.isJetpackApp &&
                blazeFeatureConfig.isEnabled() &&
                postStatus == PostStatus.PUBLISHED &&
                postModel.password.isEmpty() &&
                siteModel.isAdmin
    }
}
