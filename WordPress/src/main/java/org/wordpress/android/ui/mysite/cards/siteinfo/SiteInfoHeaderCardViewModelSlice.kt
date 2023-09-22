package org.wordpress.android.ui.mysite.cards.siteinfo

import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class SiteInfoHeaderCardViewModelSlice @Inject constructor(
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig
) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _onTechInputDialogShown = MutableLiveData<Event<MySiteViewModel.TextInputDialogModel>>()
    val onTechInputDialogShown = _onTechInputDialogShown

    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    val onBasicDialogShown = _onBasicDialogShown

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onTrackWithTabSource = MutableLiveData<Event<MySiteViewModel.MySiteTrackWithTabSource>>()
    val onTrackWithTabSource = _onTrackWithTabSource

    private val isMySiteDashboardTabsEnabled by lazy { mySiteDashboardTabsFeatureConfig.isEnabled() }

    fun getParams(
        site: SiteModel,
        activeTask: QuickStartStore.QuickStartTask? = null,
        showSiteIconProgressBar: Boolean = false
    ): MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams {
        return MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams(
            site = site,
            showSiteIconProgressBar = showSiteIconProgressBar,
            titleClick = this::titleClick,
            iconClick = this::iconClick,
            urlClick = this::urlClick,
            switchSiteClick = this::switchSiteClick,
            activeTask = activeTask
        )
    }

    private fun titleClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value =
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_network_connection)))
        } else if (!SiteUtils.isAccessedViaWPComRest(selectedSite) || !selectedSite.hasCapabilityManageOptions) {
            _onSnackbarMessage.value = Event(
                SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
            )
        } else {
            _onTechInputDialogShown.value = Event(
                MySiteViewModel.TextInputDialogModel(
                    callbackId = MySiteViewModel.SITE_NAME_CHANGE_CALLBACK_ID,
                    title = R.string.my_site_title_changer_dialog_title,
                    initialText = selectedSite.name,
                    hint = R.string.my_site_title_changer_dialog_hint,
                    isMultiline = false,
                    isInputEnabled = true
                )
            )
        }
    }

    private fun iconClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED)
        val hasIcon = selectedSite.iconUrl != null
        if (selectedSite.hasCapabilityManageOptions && selectedSite.hasCapabilityUploadFiles) {
            if (hasIcon) {
                _onBasicDialogShown.value = Event(SiteDialogModel.ChangeSiteIconDialogModel)
            } else {
                _onBasicDialogShown.value = Event(SiteDialogModel.AddSiteIconDialogModel)
            }
        } else {
            val message = when {
                !selectedSite.isUsingWpComRestApi -> {
                    R.string.my_site_icon_dialog_change_requires_jetpack_message
                }

                hasIcon -> {
                    R.string.my_site_icon_dialog_change_requires_permission_message
                }

                else -> {
                    R.string.my_site_icon_dialog_add_requires_permission_message
                }
            }
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiString.UiStringRes(message)))
        }
    }

    private fun urlClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        quickStartRepository.completeTask(
            quickStartRepository.quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL)
        )
        _onNavigation.value = Event(SiteNavigationAction.OpenSite(selectedSite))
    }

    private fun switchSiteClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(AnalyticsTracker.Stat.MY_SITE_SITE_SWITCHER_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenSitePicker(selectedSite))
    }

    private fun trackWithTabSourceIfNeeded(stat: AnalyticsTracker.Stat, properties: HashMap<String, *>? = null) {
        if (isMySiteDashboardTabsEnabled) {
            _onTrackWithTabSource.postValue(Event(MySiteViewModel.MySiteTrackWithTabSource(stat, properties)))
        } else {
            analyticsTrackerWrapper.track(stat, properties ?: emptyMap())
        }
    }
}
