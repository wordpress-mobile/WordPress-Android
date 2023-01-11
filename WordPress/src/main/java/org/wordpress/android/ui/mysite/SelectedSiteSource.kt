package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.util.filter
import org.wordpress.android.util.map
import javax.inject.Inject

class SelectedSiteSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val dispatcher: Dispatcher
) : MySiteRefreshSource<SelectedSite> {
    override val refresh = MutableLiveData(selectedSiteRepository.hasSelectedSite())

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    override fun build(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) = selectedSiteRepository.selectedSiteChange
        .filter { it == null || it.id == siteLocalId }
        .apply { onRefreshedMainThread() }
        .map { SelectedSite(it) }

    override fun refresh() {
        updateSiteSettingsIfNecessary()
        selectedSiteRepository.getSelectedSite()?.let {
            super.refresh()
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(it))
        }
    }

    fun updateSiteSettingsIfNecessary() = selectedSiteRepository.updateSiteSettingsIfNecessary()

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged?) {
        // Handled in WPMainActivity, this observe is only to manage the refresh flag
        onRefreshedMainThread()
    }
}
