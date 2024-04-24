package org.wordpress.android.fluxc.model.stats.subscribers

import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class SubscribersMapper @Inject constructor() {
    fun map(response: SubscribersRestClient.SubscribersResponse, cacheMode: LimitMode): SubscribersModel {
        val periodIndex = response.fields?.indexOf("period")
        val subscribersIndex = response.fields?.indexOf("subscribers")
        val dataPerPeriod = response.data?.mapNotNull { periodData ->
            periodData?.let {
                val period = periodIndex?.let { periodData[it] as String }
                if (!period.isNullOrBlank()) {
                    val subscribers = subscribersIndex?.let { periodData[it] as? Double } ?: 0
                    SubscribersModel.PeriodData(period, subscribers.toLong())
                } else {
                    null
                }
            }
        }?.let {
            if (cacheMode is LimitMode.Top) {
                it.take(cacheMode.limit)
            } else {
                it
            }
        }
        if (response.data == null || response.date == null || dataPerPeriod == null) {
            AppLog.e(AppLog.T.STATS, "SubscribersResponse: data, date & dataPerPeriod fields should never be null")
        }
        return SubscribersModel(response.date ?: "", dataPerPeriod ?: listOf())
    }
}
