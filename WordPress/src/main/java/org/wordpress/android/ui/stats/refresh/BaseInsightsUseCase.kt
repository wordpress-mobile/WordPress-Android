package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes

abstract class BaseInsightsUseCase(val type: InsightsTypes) {
    protected val mutableLiveData = MutableLiveData<InsightsItem>()
    val liveData: LiveData<InsightsItem> = mutableLiveData
    abstract suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean)
    fun clear() {
        mutableLiveData.postValue(null)
    }
}
