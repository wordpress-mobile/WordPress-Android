package org.wordpress.android.fluxc.model.stats.time

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor(val gson: Gson) {
    fun map(response: PostAndPageViewsResponse, pageSize: Int): PostAndPageViewsModel {
        val postViews = response.days.entries.first().value.postViews
        val stats = postViews.take(pageSize).mapNotNull { item ->
            val type = when (item.type) {
                "post" -> ViewsType.POST
                "page" -> ViewsType.PAGE
                "homepage" -> ViewsType.HOMEPAGE
                else -> {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Unexpected view type: ${item.type}")
                    null
                }
            }
            type?.let { ViewsModel(item.id, item.title, item.views, type, item.href) }
        }
        return PostAndPageViewsModel(stats, postViews.size > pageSize)
    }

    fun map(response: ReferrersResponse, pageSize: Int): ReferrersModel {
        val first = response.groups.values.first()
        val groups = first.groups.take(pageSize).map { group ->
            val children = group.referrers.mapNotNull { result ->
                if (result.name != null && result.views != null && result.icon != null && result.url != null) {
                    val firstChildUrl = result.children.firstOrNull()?.url
                    Referrer(result.name, result.views, result.icon, firstChildUrl ?: result.url)
                } else {
                    AppLog.e(STATS, "ReferrersResponse.type: Missing fields on a referrer")
                    null
                }
            }.take(pageSize)
            ReferrersModel.Group(group.groupId, group.name, group.icon, group.url, group.total, children)
        }
        return ReferrersModel(first.otherViews ?: 0, first.totalViews ?: 0, groups)
    }
}
