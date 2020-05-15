package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.SiteHomepageSettings.Page
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Posts
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront.PAGE
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront.POSTS
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse
import java.lang.IllegalStateException
import javax.inject.Inject

class SiteHomepageSettingsMapper
@Inject constructor() {
    fun map(site: SiteModel): SiteHomepageSettings? {
        val showOnFront = when (site.showOnFront) {
            PAGE.value -> PAGE
            POSTS.value -> POSTS
            else -> null
        }
        return showOnFront?.let {
            when (showOnFront) {
                PAGE -> Page(site.pageForPosts, site.pageOnFront)
                POSTS -> Posts
            }
        }
    }

    fun map(data: UpdateHomepageResponse): SiteHomepageSettings {
        return if (data.isPageOnFront) {
            val pageForPostsId = data.pageForPostsId ?: -1
            val pageOnFrontId = data.pageOnFrontId ?: -1
            Page(pageForPostsId, pageOnFrontId)
        } else {
            Posts
        }
    }
}
