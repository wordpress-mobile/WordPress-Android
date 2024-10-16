package org.wordpress.android.fluxc.store.stats.subscribers

import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel.PeriodData
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient.SubscribersResponse
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE

val SUBSCRIBERS_RESPONSE = SubscribersResponse(
    "2024-04-22",
    "day",
    listOf("period", "subscribers"),
    listOf(listOf("2024-04-21", "10"))
)
val SUBSCRIBERS_MODEL = SubscribersModel("2018-04-22", listOf(PeriodData("2024-04-22", 10)))
val INVALID_DATA_ERROR = StatsError(INVALID_RESPONSE, "Subscribers: Required data 'period' or 'dates' missing")
