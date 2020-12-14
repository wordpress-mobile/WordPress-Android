package org.wordpress.android.ui.mysite

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSite
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class MySiteViewModel @Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteInfoBlockBuilder: SiteInfoBlockBuilder,
    private val accountStore: AccountStore
) : ScopedViewModel(mainDispatcher) {
    private val _currentAccountAvatarUrl = MutableLiveData<String>()
    private val _showSiteIconProgressBar = MutableLiveData<Boolean>()
    private val _selectedSite = MutableLiveData<SiteModel>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onNavigation = MutableLiveData<Event<NavigationAction>>()

    val onSnackbarMessage = _onSnackbarMessage as LiveData<Event<SnackbarMessageHolder>>
    val onTextInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<SiteDialogModel>>
    val onNavigation = _onNavigation as LiveData<Event<NavigationAction>>
    val uiModel: LiveData<UiModel> = merge(
            _currentAccountAvatarUrl,
            _selectedSite,
            _showSiteIconProgressBar
    ) { currentAvatarUrl, site, showSiteIconProgressBar ->
        val items = if (site != null) {
            val siteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(
                    site,
                    showSiteIconProgressBar ?: false,
                    this::titleClick,
                    this::iconClick,
                    this::urlClick,
                    this::switchSiteClick
            )
            listOf<MySiteItem>(siteInfoBlock)
        } else {
            listOf()
        }
        UiModel(currentAvatarUrl.orEmpty(), items)
    }

    private fun titleClick(selectedSite: SiteModel) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(R.string.error_network_connection)))
        } else if (!SiteUtils.isAccessedViaWPComRest(selectedSite) || !selectedSite.hasCapabilityManageOptions) {
            _onSnackbarMessage.value = Event(
                    SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
            )
        } else {
            _onTechInputDialogShown.value = Event(
                    TextInputDialogModel(
                            callbackId = SITE_NAME_CHANGE_CALLBACK_ID,
                            title = R.string.my_site_title_changer_dialog_title,
                            initialText = selectedSite.name,
                            hint = R.string.my_site_title_changer_dialog_hint,
                            isMultiline = false,
                            isInputEnabled = true
                    )
            )
        }
    }

    private fun iconClick(site: SiteModel) {
        analyticsTrackerWrapper.track(MY_SITE_ICON_TAPPED)
        val hasIcon = site.iconUrl != null
        if (site.hasCapabilityManageOptions && site.hasCapabilityUploadFiles) {
            if (hasIcon) {
                _onBasicDialogShown.value = Event(ChangeSiteIconDialogModel)
            } else {
                _onBasicDialogShown.value = Event(AddSiteIconDialogModel)
            }
        } else {
            val message = when {
                !site.isUsingWpComRestApi -> {
                    R.string.my_site_icon_dialog_change_requires_jetpack_message
                }
                hasIcon -> {
                    R.string.my_site_icon_dialog_change_requires_permission_message
                }
                else -> {
                    R.string.my_site_icon_dialog_add_requires_permission_message
                }
            }
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(message)))
        }
    }

    private fun urlClick(site: SiteModel) {
        _onNavigation.value = Event(OpenSite(site))
    }

    private fun switchSiteClick(site: SiteModel) {
        _onNavigation.value = Event(OpenSitePicker(site))
    }

    fun updateSite(selectedSite: SiteModel?) {
        _selectedSite.value = selectedSite
    }

    fun refreshAccountAvatarUrl() {
        _currentAccountAvatarUrl.value = accountStore.account?.avatarUrl.orEmpty()
    }

    fun onSiteNameChosen(input: String?) {
        TODO("Not yet implemented")
    }

    fun onSiteNameChooserDismissed() {
        TODO("Not yet implemented")
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        TODO("Not yet implemented")
    }

    fun onAvatarPressed() {
        _onNavigation.value = Event(OpenMeScreen)
    }

    data class UiModel(
        val accountAvatarUrl: String,
        val items: List<MySiteItem>
    )

    data class TextInputDialogModel(
        val callbackId: Int = SITE_NAME_CHANGE_CALLBACK_ID,
        @StringRes val title: Int,
        val initialText: String,
        @StringRes val hint: Int,
        val isMultiline: Boolean,
        val isInputEnabled: Boolean
    )

    sealed class NavigationAction {
        object OpenMeScreen : NavigationAction()
        data class OpenSite(val site: SiteModel) : NavigationAction()
        data class OpenSitePicker(val site: SiteModel) : NavigationAction()
    }

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG = "TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
    }
}
