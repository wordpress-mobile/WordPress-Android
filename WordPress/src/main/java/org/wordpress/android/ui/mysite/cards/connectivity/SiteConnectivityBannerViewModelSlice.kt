package org.wordpress.android.ui.mysite.cards.connectivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.ui.accounts.login.CredentialsChangedNotifier
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class SiteConnectivityBannerViewModelSlice @Inject constructor(
    private val editorSettingsRepository: EditorSettingsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val credentialsChangedNotifier: CredentialsChangedNotifier,
    private val selectedSiteRepository: SelectedSiteRepository,
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
        // Re-run detection the moment an application password is established for the selected site
        // (e.g. the headless mint finished after our first fetch lost the race), instead of waiting
        // for the next resume/refresh. Re-read the selected site so we see the just-persisted
        // credentials; isUserInitiated = false so a replayed event is a no-op once cached.
        scope.launch {
            credentialsChangedNotifier.events.collect { siteLocalId ->
                val site = selectedSiteRepository.getSelectedSite()
                if (site != null && site.id == siteLocalId) {
                    fetchCapabilities(site, isUserInitiated = false)
                }
            }
        }
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
            // Atomic sites probe the direct host with an application password that's minted
            // asynchronously on this same screen, so a first-login fetch can fail purely because
            // the credential isn't ready yet. Treat that as pending, not a connection failure —
            // the application-password card owns that state and a later fetch will succeed.
            val suppressForPendingAuth =
                !ok && editorSettingsRepository.isAwaitingApplicationPassword(site)
            // Show the banner only as a last resort — not when detection succeeded, when we have
            // cached capabilities, or while a transient non-error state (offline / pending creds)
            // already explains the failure.
            val suppressBanner = ok || hasCache || suppressForOffline || suppressForPendingAuth
            _uiModel.postValue(if (suppressBanner) null else buildBanner())
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
