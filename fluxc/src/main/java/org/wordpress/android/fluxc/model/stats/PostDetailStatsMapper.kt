package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import javax.inject.Inject

class PostDetailStatsMapper @Inject constructor() {
    @Suppress("ComplexCondition")
    fun map(postStatsResponse: PostStatsResponse): PostDetailStatsModel {
        val daysViews = if (
            postStatsResponse.fields != null &&
            postStatsResponse.data != null &&
            postStatsResponse.fields.size > 1 &&
            postStatsResponse.fields[0] == "period" &&
            postStatsResponse.fields[1] == "views"
        ) {
            postStatsResponse.data.map { list -> PostDetailStatsModel.Day(list[0], list[1].toInt()) }
        } else {
            listOf()
        }
        val weekViews = postStatsResponse.weeks.map { week ->
            PostDetailStatsModel.Week(
                week.days.map { day ->
                    PostDetailStatsModel.Day(
                        day.day,
                        day.count ?: 0
                    )
                },
                week.average ?: 0,
                week.total ?: 0
            )
        }
        val yearTotals = postStatsResponse.years.map { (year, model) ->
            PostDetailStatsModel.Year(
                year,
                model.months.map { (month, value) -> PostDetailStatsModel.Month(month, value) },
                model.total ?: 0
            )
        }
        val yearAverages = postStatsResponse.averages.map { (year, model) ->
            PostDetailStatsModel.Year(
                year,
                model.months.map { (month, value) -> PostDetailStatsModel.Month(month, value) },
                model.overall ?: 0
            )
        }
        return PostDetailStatsModel(postStatsResponse.views ?: 0, daysViews, weekViews, yearTotals, yearAverages)
    }
}
