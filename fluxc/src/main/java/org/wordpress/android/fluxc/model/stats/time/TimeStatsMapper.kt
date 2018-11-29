package org.wordpress.android.fluxc.model.stats.time

import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor() {
    fun map(response: PostAndPageViewsResponse, pageSize: Int): PostAndPageViewsModel {
        val postViews = response.days.entries.first().value.postViews
        val stats = postViews.take(pageSize).mapNotNull { item ->
            val type = when (item.type) {
                "post" -> ViewsType.POST
                "page" -> ViewsType.PAGE
                else -> {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Unexpected view type: ${item.type}")
                    null
                }
            }
            type?.let { ViewsModel(item.title, item.views, type) }
        }
        return PostAndPageViewsModel(stats, postViews.size > pageSize)
    }
}
