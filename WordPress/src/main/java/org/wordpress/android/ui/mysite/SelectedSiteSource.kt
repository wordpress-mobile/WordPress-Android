package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.util.filter
import org.wordpress.android.util.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedSiteSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val dispatcher: Dispatcher
) : MySiteSource<SelectedSite> {
    val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun build(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) = selectedSiteRepository.selectedSiteChange
            .filter { it == null || it.id == siteLocalId }
            .map { SelectedSite(it) }

    fun refresh() {
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        selectedSiteRepository.getSelectedSite()?.let {
            refresh.postValue(true)
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(it))
        }
    }

    fun isRefreshing() = refresh.value as Boolean

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged?) {
        // Handled in WPMainActivity, this observe is only to manage the refresh flag
        refresh.value = false
    }
}
