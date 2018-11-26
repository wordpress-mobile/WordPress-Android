package org.wordpress.android.fluxc.model.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor() {
    fun map(response: PostAndPageViewsResponse, site: SiteModel): PostAndPageViewsModel {
        val stats = response.days.entries.first().value.postViews.map {
            ViewsModel(it.title, it.views, ViewsType.valueOf(it.type))
        }
        return PostAndPageViewsModel(stats)
    }
}
