package org.wordpress.android.ui.mysite.cards.connectivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.EditorCapabilityDetectionState
import org.wordpress.android.repositories.EditorCapabilityDetector
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import javax.inject.Inject

class SiteConnectivityBannerViewModelSlice @Inject constructor(
    private val editorCapabilityDetector: EditorCapabilityDetector,
) {
    private lateinit var scope: CoroutineScope
    private var collectJob: Job? = null
    private var currentSite: SiteModel? = null

    private val _uiModel = MutableLiveData<MySiteCardAndItem?>()
    val uiModel: LiveData<MySiteCardAndItem?> = _uiModel

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    /**
     * Subscribes the banner to [site]'s editor-capability detection state. The
     * banner is a thin view over that state — it surfaces only when detection
     * reports the site [Unreachable][EditorCapabilityDetectionState.Unreachable].
     * Every other state (probing, pending credentials, offline, ready) leaves it
     * hidden, so the dedup, offline-suppression, and pending-credential handling
     * that used to live here now belong to the one detector. [isUserInitiated]
     * (pull-to-refresh, banner retry) forces a fresh probe.
     */
    fun fetchCapabilities(site: SiteModel, isUserInitiated: Boolean) {
        collectJob?.cancel()
        currentSite = site
        if (isUserInitiated) editorCapabilityDetector.refresh(site)
        collectJob = scope.launch {
            editorCapabilityDetector.stateFor(site).collect { state ->
                // Bail if the user switched sites while suspended — postValue is
                // not a suspension point, so cancellation alone won't catch this.
                if (currentSite?.id != site.id) return@collect
                val showBanner = state is EditorCapabilityDetectionState.Unreachable
                _uiModel.postValue(if (showBanner) buildBanner() else null)
            }
        }
    }

    fun clearBanner() {
        collectJob?.cancel()
        currentSite = null
        _uiModel.postValue(null)
    }

    private fun buildBanner(): MySiteCardAndItem.Item.SingleActionCard =
        MySiteCardAndItem.Item.SingleActionCard(
            textResource = R.string.site_connectivity_banner_text,
            imageResource = R.drawable.ic_cloud_off_themed_24dp,
            onActionClick = {
                currentSite?.let { fetchCapabilities(it, isUserInitiated = true) }
            },
            showLearnMore = false,
            centerImageVertically = true,
        )
}
