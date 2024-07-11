package org.wordpress.android.fluxc.model.stats.subscribers

data class SubscribersModel(val period: String, val dates: List<PeriodData>) {
    data class PeriodData(val period: String, val subscribers: Long)
}
