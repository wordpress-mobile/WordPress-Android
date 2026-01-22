package org.wordpress.android.ui.main

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.image.BlavatarShape
import org.wordpress.android.util.image.ImageType

/**
 * SiteRecord is a simplified version of the full account (blog) record
 */
class SiteRecord(siteModel: SiteModel) {
    val localId: Int
    val siteId: Long
    val blogName: String
    val homeURL: String
    val blavatarUrl: String
    val blavatarType: ImageType
    val isSelfHostedSite: Boolean
    var isRecentPick = false

    init {
        localId = siteModel.id
        siteId = siteModel.siteId
        blogName = SiteUtils.getSiteNameOrHomeURL(siteModel)
        homeURL = SiteUtils.getHomeURLOrHostName(siteModel)
        blavatarUrl = SiteUtils.getSiteIconUrl(siteModel, SitePickerAdapter.mBlavatarSz)
        blavatarType = SiteUtils.getSiteImageType(
            siteModel.isWpForTeamsSite, BlavatarShape.SQUARE_WITH_ROUNDED_CORNERES
        )
        isSelfHostedSite = !siteModel.isUsingWpComRestApi
    }

    val blogNameOrHomeURL: String
        get() = blogName.ifEmpty {
            homeURL
        }
}
