package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes

abstract class BaseInsightsUseCase(val type: InsightsTypes) {
    abstract val liveData: LiveData<InsightsItem>
    abstract suspend fun fetch(site: SiteModel, forced: Boolean)
}
