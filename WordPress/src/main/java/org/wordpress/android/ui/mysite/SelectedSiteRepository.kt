package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.SiteSettingsInterfaceWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedSiteRepository
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteSettingsInterfaceFactory: SiteSettingsInterfaceWrapper.Factory
) {
    private var siteSettings: SiteSettingsInterfaceWrapper? = null
    private val _selectedSiteChange = MutableLiveData<SiteModel>()
    private val _siteSelected = MutableLiveData<Int>()
    private val _showSiteIconProgressBar = MutableLiveData<Boolean>()
    val selectedSiteChange = _selectedSiteChange as LiveData<SiteModel>
    val siteSelected = _siteSelected as LiveData<Int?>
    val showSiteIconProgressBar = _showSiteIconProgressBar as LiveData<Boolean>
    fun updateSite(selectedSite: SiteModel?) {
        if (getSelectedSite()?.iconUrl != selectedSite?.iconUrl) {
            showSiteIconProgressBar(false)
        }
        if (selectedSite?.id != _siteSelected.value) {
            _siteSelected.value = selectedSite?.id
        }
        _selectedSiteChange.value = selectedSite
    }

    fun updateSiteIconMediaId(mediaId: Int, showProgressBar: Boolean) {
        siteSettings?.let {
            showSiteIconProgressBar(showProgressBar)
            it.setSiteIconMediaId(mediaId)
            it.saveSettings()
        }
    }

    fun showSiteIconProgressBar(progressBarVisible: Boolean) {
        if (_showSiteIconProgressBar.value != progressBarVisible) {
            _showSiteIconProgressBar.postValue(progressBarVisible)
        }
    }

    fun updateTitle(title: String) {
        siteSettings?.let {
            getSelectedSite()?.name = title
            dispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(getSelectedSite()))
            it.title = title
            it.saveSettings()
        }
    }

    fun clear() {
        siteSettings?.clear()
    }

    fun hasSelectedSite() = _selectedSiteChange.value != null

    fun getSelectedSite() = _selectedSiteChange.value

    fun updateSiteSettingsIfNecessary() {
        // If the selected site is null, we can't update its site settings
        val selectedSite = getSelectedSite() ?: return
        if (siteSettings != null && siteSettings!!.localSiteId != selectedSite.id) {
            // The site has changed, we can't use the previous site settings, force a refresh
            siteSettings?.clear()
            siteSettings = null
        }
        if (siteSettings == null) {
            fun onError(error: Exception?) {
                showSiteIconProgressBar(false)
            }
            siteSettings = siteSettingsInterfaceFactory.build(
                    selectedSite,
                    onSaveError = ::onError,
                    onFetchError = ::onError,
                    onSettingsSaved = {
                        getSelectedSite()?.let { site ->
                            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
                        }
                    }
            )

            siteSettings?.init(true)
        }
    }

    fun isSiteIconUploadInProgress(): Boolean {
        return _showSiteIconProgressBar.value == true
    }
}
