package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.store.StatsStore.InsightType

data class InsightTypeModel(val addedTypes: List<InsightType>, val removedTypes: List<InsightType>)
