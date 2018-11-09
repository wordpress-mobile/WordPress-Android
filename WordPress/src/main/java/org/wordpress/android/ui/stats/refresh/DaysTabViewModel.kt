package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import javax.inject.Inject
import javax.inject.Named

class DaysTabViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : StatsListViewModel(mainDispatcher) {
    override val data: LiveData<InsightsUiState> = MutableLiveData()

    override fun start(site: SiteModel) {
        reload(site)
    }
}
