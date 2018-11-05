package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.android.HandlerDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.modules.UI_THREAD
import javax.inject.Inject
import javax.inject.Named

class DaysTabViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: HandlerDispatcher
) : StatsListViewModel(mainDispatcher) {
    override val data: LiveData<InsightsUiState> = MutableLiveData()

    override fun start(site: SiteModel) {
        reload(site)
    }

    override fun reload(site: SiteModel) {
    }
}
