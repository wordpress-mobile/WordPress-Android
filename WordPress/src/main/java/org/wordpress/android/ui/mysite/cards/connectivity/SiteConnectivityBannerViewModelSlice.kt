package org.wordpress.android.ui.mysite.cards.connectivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class SiteConnectivityBannerViewModelSlice @Inject constructor(
    private val editorSettingsRepository: EditorSettingsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) {
    private lateinit var scope: CoroutineScope
    private var currentJob: Job? = null
    private var currentSite: SiteModel? = null

    private val _uiModel = MutableLiveData<MySiteCardAndItem?>()
    val uiModel: LiveData<MySiteCardAndItem?> = _uiModel

    /* Site capabilities rarely change, so once we've successfully fetched them for a site we
       skip subsequent non-user-initiated fetches in this slice's lifetime. Failed fetches do
       not populate this set, so a transient network failure recovers on the next onResume.
       User-initiated calls (PTR, banner retry) always bypass this gate. */
    private val fetchedCapabilitiesForSite = mutableSetOf<Int>()

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun fetchCapabilities(site: SiteModel, isUserInitiated: Boolean) {
        currentJob?.cancel()
        currentSite = site
        currentJob = scope.launch {
            if (site.id in fetchedCapabilitiesForSite && !isUserInitiated) {
                return@launch
            }
            val ok = editorSettingsRepository.fetchEditorCapabilitiesForSite(site)
            if (ok) {
                fetchedCapabilitiesForSite.add(site.id)
            }
            val hasCache = editorSettingsRepository.hasCachedCapabilities(site)
            // Bail if the user switched sites while we were suspended — postValue
            // isn't a suspension point, so cancellation alone won't catch this.
            if (currentSite?.id != site.id) return@launch
            // Suppress the banner when the device is offline — the global "no
            // connection" banner already covers this case, and stacking warnings
            // for the same root cause is just noise.
            val suppressForOffline = !ok && !networkUtilsWrapper.isNetworkAvailable()
            _uiModel.postValue(if (ok || hasCache || suppressForOffline) null else buildBanner())
        }
    }

    fun clearBanner() {
        currentJob?.cancel()
        currentSite = null
        _uiModel.postValue(null)
    }

    private fun buildBanner(): MySiteCardAndItem.Item.SingleActionCard =
        MySiteCardAndItem.Item.SingleActionCard(
            textResource = R.string.site_connectivity_banner_text,
            imageResource = R.drawable.ic_cloud_off_themed_24dp,
            onActionClick = {
                val site = currentSite
                if (site != null && currentJob?.isActive != true) {
                    fetchCapabilities(site, isUserInitiated = true)
                }
            },
            showLearnMore = false,
            centerImageVertically = true,
        )
}
