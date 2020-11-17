package org.wordpress.android.ui.mysite

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteViewModel.BasicDialogModel.Type.ADD_SITE_ICON
import org.wordpress.android.ui.mysite.MySiteViewModel.BasicDialogModel.Type.CHANGE_SITE_ICON
import org.wordpress.android.ui.mysite.MySiteViewModel.BasicDialogModel.Type.EDIT_SITE_NOT_ALLOWED
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSite
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel.Type.UPDATE_TITLE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class MySiteViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _showSiteIconProgressBar = MutableLiveData<Boolean>()
    private val _selectedSite = MutableLiveData<SiteModel>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<BasicDialogModel>>()
    private val _onNavigation = MutableLiveData<Event<NavigationAction>>()

    val onSnackbarMessage = _onSnackbarMessage as LiveData<Event<SnackbarMessageHolder>>
    val onTechInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<BasicDialogModel>>
    val onNavigation = _onNavigation as LiveData<Event<NavigationAction>>
    val uiModel: LiveData<List<MySiteItem>> = merge(
            _selectedSite,
            _showSiteIconProgressBar
    ) { site, showSiteIconProgressBar ->
        if (site != null) {
            val homeUrl = SiteUtils.getHomeURLOrHostName(site)
            val blogTitle = SiteUtils.getSiteNameOrHomeURL(site)
            val siteIcon = if (showSiteIconProgressBar != true && !site.iconUrl.isNullOrEmpty()) {
                SiteUtils.getSiteIconUrl(
                        site,
                        resourceProvider.getDimensionPixelSize(R.dimen.blavatar_sz_small)
                )
            } else {
                null
            }
            listOf<MySiteItem>(
                    SiteInfoBlock(
                            blogTitle,
                            homeUrl,
                            siteIcon,
                            buildTitleClick(site),
                            ListItemInteraction.create(site, this::siteClick),
                            ListItemInteraction.create(site, this::urlClick),
                            ListItemInteraction.create(site, this::switchSiteClick)
                    )
            )
        } else {
            listOf()
        }
    }

    private fun buildTitleClick(site: SiteModel): ListItemInteraction? {
        return if (SiteUtils.isAccessedViaWPComRest(site)) {
            ListItemInteraction.create(site, this::titleClick)
        } else {
            null
        }
    }

    private fun titleClick(selectedSite: SiteModel) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(R.string.error_network_connection)))
        } else {
            val canEditTitle = SiteUtils.isAccessedViaWPComRest(selectedSite) && selectedSite.hasCapabilityManageOptions
            val hint = if (canEditTitle) {
                R.string.my_site_title_changer_dialog_hint
            } else {
                R.string.my_site_title_changer_dialog_not_allowed_hint
            }
            _onTechInputDialogShown.value = Event(
                    TextInputDialogModel(
                            R.string.my_site_title_changer_dialog_title,
                            selectedSite.name,
                            hint,
                            false,
                            canEditTitle,
                            UPDATE_TITLE
                    )
            )
        }
    }

    private fun siteClick(site: SiteModel) {
        analyticsTrackerWrapper.track(MY_SITE_ICON_TAPPED)
        val hasIcon = site.iconUrl != null
        if (site.hasCapabilityManageOptions && site.hasCapabilityUploadFiles) {
            if (hasIcon) {
                _onBasicDialogShown.value = Event(
                        BasicDialogModel(
                                CHANGE_SITE_ICON,
                                R.string.my_site_icon_dialog_title,
                                R.string.my_site_icon_dialog_change_message,
                                R.string.my_site_icon_dialog_change_button,
                                R.string.my_site_icon_dialog_remove_button,
                                R.string.my_site_icon_dialog_cancel_button
                        )
                )
            } else {
                _onBasicDialogShown.value = Event(
                        BasicDialogModel(
                                ADD_SITE_ICON,
                                R.string.my_site_icon_dialog_title,
                                R.string.my_site_icon_dialog_add_message,
                                R.string.yes,
                                R.string.no,
                                null
                        )
                )
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
            _onBasicDialogShown.value = Event(
                    BasicDialogModel(
                            EDIT_SITE_NOT_ALLOWED,
                            R.string.my_site_icon_dialog_title,
                            message,
                            R.string.dialog_button_ok,
                            null,
                            null
                    )
            )
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

    fun onSiteNameChosen(input: String?) {
        TODO("Not yet implemented")
    }

    fun onSiteNameChooserDismissed() {
        TODO("Not yet implemented")
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        TODO("Not yet implemented")
    }

    data class TextInputDialogModel(
        @StringRes val title: Int,
        val initialText: String,
        @StringRes val hint: Int,
        val isMultiline: Boolean,
        val isInputEnabled: Boolean,
        val type: Type
    ) {
        enum class Type(val callbackId: Int) {
            UPDATE_TITLE(1)
        }
    }

    data class BasicDialogModel(
        val type: Type,
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val positiveButtonLabel: Int,
        @StringRes val negativeButtonLabel: Int? = null,
        @StringRes val cancelButtonLabel: Int? = null
    ) {
        enum class Type(val tag: String) {
            CHANGE_SITE_ICON(TAG_CHANGE_SITE_ICON_DIALOG),
            ADD_SITE_ICON(TAG_ADD_SITE_ICON_DIALOG),
            EDIT_SITE_NOT_ALLOWED(TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG)
        }
    }

    sealed class NavigationAction {
        data class OpenSite(val site: SiteModel) : NavigationAction()
        data class OpenSitePicker(val site: SiteModel) : NavigationAction()
    }

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG = "TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG"
    }
}
