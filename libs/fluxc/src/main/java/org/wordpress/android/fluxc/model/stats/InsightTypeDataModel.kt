package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes

data class InsightTypeDataModel(val type: InsightsTypes, val status: Status, val position: Int?) {
    enum class Status {
        ADDED, REMOVED, NEW
    }
}
