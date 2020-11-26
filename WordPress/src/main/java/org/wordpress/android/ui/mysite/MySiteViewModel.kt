package org.wordpress.android.ui.mysite

import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_CROPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_GALLERY_PICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_REMOVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_SHOT_NEW
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_STATS_TAPPED
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenConnectJetpackForStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenCropActivity
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSite
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.OpenStatsScreen
import org.wordpress.android.ui.mysite.MySiteViewModel.NavigationAction.StartLoginForJetpackStats
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.distinct
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class MySiteViewModel
@Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteInfoBlockBuilder: SiteInfoBlockBuilder,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val contextProvider: ContextProvider
) : ScopedViewModel(mainDispatcher) {
    private val _currentAccountAvatarUrl = MutableLiveData<String>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onNavigation = MutableLiveData<Event<NavigationAction>>()
    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()

    val onSnackbarMessage = _onSnackbarMessage as LiveData<Event<SnackbarMessageHolder>>
    val onTextInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<SiteDialogModel>>
    val onNavigation = _onNavigation as LiveData<Event<NavigationAction>>
    val onMediaUpload = _onMediaUpload as LiveData<Event<MediaModel>>
    val uiModel: LiveData<UiModel> = merge(
            _currentAccountAvatarUrl,
            selectedSiteRepository.selectedSiteChange,
            selectedSiteRepository.showSiteIconProgressBar.distinct()
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
            val quickActionsBlock = QuickActionsBlock(
                    ListItemInteraction.create(site) { statsClick(site, true) },
                    ListItemInteraction.create(site) { pagesClick(site, true) },
                    ListItemInteraction.create(site) { postsClick(site, true) },
                    ListItemInteraction.create(site) { mediaClick(site, true) },
                    site.isSelfHostedAdmin || site.hasCapabilityEditPages
            )
            listOf(siteInfoBlock, quickActionsBlock)
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

    private fun statsClick(site: SiteModel, isFromQuickActions: Boolean) {
        if (isFromQuickActions) {
            analyticsTrackerWrapper.track(QUICK_ACTION_STATS_TAPPED)
        }
        if (!accountStore.hasAccessToken() && site.isJetpackConnected) {
            // If the user is not connected to WordPress.com, ask him to connect first.
            _onNavigation.value = Event(StartLoginForJetpackStats)
        } else if (site.isWPCom || site.isJetpackInstalled && site.isJetpackConnected) {
            _onNavigation.value = Event(OpenStatsScreen(site))
        } else {
            _onNavigation.value = Event(OpenConnectJetpackForStatsScreen(site))
        }
    }

    private fun pagesClick(site: SiteModel, isFromQuickActions: Boolean) {
        TODO()
    }

    private fun postsClick(site: SiteModel, isFromQuickActions: Boolean) {
        TODO()
    }

    private fun mediaClick(site: SiteModel, isFromQuickActions: Boolean) {
        TODO()
    }

    fun refreshAccountAvatarUrl() {
        _currentAccountAvatarUrl.value = accountStore.account?.avatarUrl.orEmpty()
    }

    fun onSiteNameChosen(input: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
            )
        } else {
            selectedSiteRepository.updateTitle(input)
        }
    }

    fun onSiteNameChooserDismissed() {
        // do nothing
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        when (interaction) {
            is Positive -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    _onNavigation.postValue(
                            Event(OpenMediaPicker(requireNotNull(selectedSiteRepository.getSelectedSite())))
                    )
                }
            }
            is Negative -> when (interaction.tag) {
                TAG_CHANGE_SITE_ICON_DIALOG -> {
                    analyticsTrackerWrapper.track(MY_SITE_ICON_REMOVED)
                    selectedSiteRepository.updateSiteIconMediaId(0, true)
                }
            }
            is Dismissed -> {
                // do nothing
            }
        }
    }

    fun handleTakenSiteIcon(iconUrl: String?, source: PhotoPickerMediaSource?) {
        val stat = if (source == ANDROID_CAMERA) MY_SITE_ICON_SHOT_NEW else MY_SITE_ICON_GALLERY_PICKED
        analyticsTrackerWrapper.track(stat)
        val imageUri = Uri.parse(iconUrl)?.let { UriWrapper(it) }
        if (imageUri != null) {
            selectedSiteRepository.showSiteIconProgressBar(true)
            launch(bgDispatcher) {
                val fetchMedia = wpMediaUtilsWrapper.fetchMediaToUriWrapper(imageUri)
                if (fetchMedia != null) {
                    _onNavigation.postValue(Event(OpenCropActivity(fetchMedia)))
                } else {
                    selectedSiteRepository.showSiteIconProgressBar(false)
                }
            }
        }
    }

    fun handleCropResult(croppedUri: Uri?, success: Boolean) {
        if (success && croppedUri != null) {
            analyticsTrackerWrapper.track(MY_SITE_ICON_CROPPED)
            selectedSiteRepository.showSiteIconProgressBar(true)
            launch(bgDispatcher) {
                wpMediaUtilsWrapper.fetchMediaToUriWrapper(UriWrapper(croppedUri))?.let { fetchMedia ->
                    mediaUtilsWrapper.getRealPathFromURI(fetchMedia.uri)
                }?.let {
                    startSiteIconUpload(it)
                }
            }
        } else {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_cropping_image))))
        }
    }

    fun handleSuccessfulLoginResult() {
        selectedSiteRepository.getSelectedSite()?.let { site -> _onNavigation.value = Event(OpenStatsScreen(site)) }
    }

    private fun startSiteIconUpload(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_locating_image))))
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.file_error_create))))
            return
        }
        val site = selectedSiteRepository.getSelectedSite()
        if (site != null) {
            val media = buildMediaModel(file, site)
            if (media == null) {
                _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.file_not_found))))
                return
            }
            _onMediaUpload.postValue(Event(media))
        } else {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_generic))))
        }
    }

    private fun buildMediaModel(file: File, site: SiteModel): MediaModel? {
        val uri = Uri.Builder().path(file.path).build()
        val mimeType = contextProvider.getContext().contentResolver.getType(uri)
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)
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
        data class OpenMediaPicker(val site: SiteModel) : NavigationAction()
        data class OpenCropActivity(val imageUri: UriWrapper) : NavigationAction()
        data class OpenStatsScreen(val site: SiteModel) : NavigationAction()
        data class OpenConnectJetpackForStatsScreen(val site: SiteModel) : NavigationAction()
        object StartLoginForJetpackStats : NavigationAction()
    }

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG = "TAG_EDIT_SITE_ICON_NOT_ALLOWED_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
    }
}
