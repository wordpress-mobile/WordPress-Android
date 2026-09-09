package org.wordpress.android.ui.mysite.cards.connectivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.SiteProvisioningSource
import org.wordpress.android.repositories.SiteReadiness
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import javax.inject.Inject

class SiteConnectivityBannerViewModelSlice @Inject constructor(
    private val siteProvisioningSource: SiteProvisioningSource,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    connectionStatus: LiveData<ConnectionStatus>,
) {
    private lateinit var scope: CoroutineScope
    private var collectJob: Job? = null
    private var currentSite: SiteModel? = null

    private val readiness = MutableLiveData<SiteReadiness?>()

    private val _uiModel = MediatorLiveData<MySiteCardAndItem?>()
    val uiModel: LiveData<MySiteCardAndItem?> = _uiModel

    init {
        _uiModel.addSource(readiness) { render() }
        // Losing the network doesn't re-run the pipeline, so the readiness we hold stays Unreachable
        // and the banner would sit there stacked on the global "no connection" bar. This source is a
        // change trigger only — it emits on transitions and swallows its initial value — so the
        // decision reads the live availability rather than the emitted status.
        _uiModel.addSource(connectionStatus) { render() }
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    /**
     * Subscribes the banner to [site]'s readiness. The banner is a thin view over
     * that state — it surfaces only on [SiteReadiness.Unreachable], which the pipeline
     * reports both when the capability probe failed and when the site couldn't be
     * reached at the auth stage while the device was online. Every other state
     * (probing, needs auth, offline, ready) leaves it hidden: when credentials are the
     * problem the application-password card owns it, and the banner stays out of the
     * way. [isUserInitiated] (pull-to-refresh, retry) forces a fresh run.
     */
    fun fetchCapabilities(site: SiteModel, isUserInitiated: Boolean) {
        collectJob?.cancel()
        currentSite = site
        if (isUserInitiated) siteProvisioningSource.invalidate(site)
        collectJob = scope.launch {
            siteProvisioningSource.stateFor(site).collect { state ->
                // Bail if the user switched sites while suspended — postValue is
                // not a suspension point, so cancellation alone won't catch this.
                if (currentSite?.id != site.id) return@collect
                readiness.postValue(state)
            }
        }
    }

    fun clearBanner() {
        collectJob?.cancel()
        currentSite = null
        readiness.postValue(null)
    }

    private fun render() {
        val showBanner = readiness.value is SiteReadiness.Unreachable &&
            networkUtilsWrapper.isNetworkAvailable()
        _uiModel.value = if (showBanner) buildBanner() else null
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
