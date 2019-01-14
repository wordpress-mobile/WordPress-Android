package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes

data class InsightTypesModel(val addedTypes: List<InsightsTypes>, val removedTypes: List<InsightsTypes>)
