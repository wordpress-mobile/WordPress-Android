package org.wordpress.android.ui.mysite.cards.connectivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.SiteProvisioningSource
import org.wordpress.android.repositories.SiteReadiness
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import javax.inject.Inject

class SiteConnectivityBannerViewModelSlice @Inject constructor(
    private val siteProvisioningSource: SiteProvisioningSource,
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
     * Subscribes the banner to [site]'s readiness. The banner is a thin view over
     * that state — it surfaces only when the site is provisioned but the capability
     * probe failed ([SiteReadiness.Unreachable]). Every other state (probing, needs
     * auth, offline, ready) leaves it hidden: when credentials are the problem the
     * application-password card owns it, and the banner stays out of the way.
     * [isUserInitiated] (pull-to-refresh, retry) forces a fresh run.
     */
    fun fetchCapabilities(site: SiteModel, isUserInitiated: Boolean) {
        collectJob?.cancel()
        currentSite = site
        if (isUserInitiated) siteProvisioningSource.invalidate(site)
        collectJob = scope.launch {
            siteProvisioningSource.stateFor(site).collect { readiness ->
                // Bail if the user switched sites while suspended — postValue is
                // not a suspension point, so cancellation alone won't catch this.
                if (currentSite?.id != site.id) return@collect
                val showBanner = readiness is SiteReadiness.Unreachable
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
