package org.wordpress.android.fluxc.model.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor() {
    fun map(response: PostAndPageViewsResponse, site: SiteModel, pageSize: Int): PostAndPageViewsModel {
        val postViews = response.days.entries.first().value.postViews
        val stats = postViews.take(pageSize).map {
            ViewsModel(it.title, it.views, ViewsType.valueOf(it.type))
        }
        return PostAndPageViewsModel(stats, postViews.size > pageSize )
    }
}
