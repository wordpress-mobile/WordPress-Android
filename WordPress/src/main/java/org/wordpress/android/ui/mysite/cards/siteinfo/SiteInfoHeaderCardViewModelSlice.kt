@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.mysite.cards.siteinfo

import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class SiteInfoHeaderCardViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder
) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _onTextInputDialogShown = MutableLiveData<Event<MySiteViewModel.TextInputDialogModel>>()
    val onTextInputDialogShown = _onTextInputDialogShown

    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    val onBasicDialogShown = _onBasicDialogShown

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()
    val onMediaUpload = _onMediaUpload

    val uiModel: LiveData<SiteInfoHeaderCard?> =
        merge(quickStartRepository.activeTask,
            selectedSiteRepository.showSiteIconProgressBar,
            selectedSiteRepository.selectedSiteChange)
        { activeTask, showSiteIconProgressBar, site ->
            val siteHeaderCard = buildCard(activeTask, showSiteIconProgressBar, site)
            siteHeaderCard
        }.distinctUntilChanged()

    private lateinit var scope: CoroutineScope

    private var uploadIconJob: Job? = null

    fun initialize(viewModelScope: CoroutineScope) {
        this.scope = viewModelScope
    }

    fun buildCard(siteModel: SiteModel) {
        buildCard(null, null, siteModel = siteModel)
    }

    private fun buildCard(
        activeTask: QuickStartStore.QuickStartTask?,
        showSiteIconProgressBar: Boolean?,
        siteModel: SiteModel?
    ): SiteInfoHeaderCard? {
        siteModel?.let { site ->
            return siteInfoHeaderCardBuilder.buildSiteInfoCard(
                getParams(
                    site,
                    activeTask,
                    showSiteIconProgressBar?: false
                )
            )
        }?: return null
    }

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
            _onTextInputDialogShown.value = Event(
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
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_SITE_SWITCHER_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenSitePicker(selectedSite))
    }

    fun onSiteNameChosen(input: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_update_site_title_network)))
            )
        } else {
            selectedSiteRepository.updateTitle(input)
        }
    }

    fun onSiteNameChooserDismissed() {
        // This callback is called even when the dialog interaction is positive,
        // otherwise we would need to call 'completeTask' on 'onSiteNameChosen' as well.
        quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.UPDATE_SITE_TITLE)
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun onDialogInteraction(interaction: BasicDialogViewModel.DialogInteraction) {
        when (interaction) {
            is BasicDialogViewModel.DialogInteraction.Positive -> when (interaction.tag) {
                MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG, MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
                    _onNavigation.postValue(
                        Event(
                            SiteNavigationAction.OpenMediaPicker(
                                requireNotNull(selectedSiteRepository.getSelectedSite())
                            )
                        )
                    )
                }
            }

            is BasicDialogViewModel.DialogInteraction.Negative -> when (interaction.tag) {
                MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }

                MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG -> {
                    analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_ICON_REMOVED)
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                    selectedSiteRepository.updateSiteIconMediaId(0, true)
                }
            }

            is BasicDialogViewModel.DialogInteraction.Dismissed -> when (interaction.tag) {
                MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG, MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun handleTakenSiteIcon(iconUrl: String?, source: PhotoPickerActivity.PhotoPickerMediaSource?) {
        val stat = if (source == PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA) {
            AnalyticsTracker.Stat.MY_SITE_ICON_SHOT_NEW
        } else {
            AnalyticsTracker.Stat.MY_SITE_ICON_GALLERY_PICKED
        }
        analyticsTrackerWrapper.track(stat)
        val imageUri = Uri.parse(iconUrl)?.let { UriWrapper(it) }
        if (imageUri != null) {
            scope.launch(bgDispatcher) {
                val fetchMedia = wpMediaUtilsWrapper.fetchMediaToUriWrapper(imageUri)
                if (fetchMedia != null) {
                    _onNavigation.postValue(Event(SiteNavigationAction.OpenCropActivity(fetchMedia)))
                }
            }
        }
    }

    fun handleSelectedSiteIcon(mediaId: Long) {
        selectedSiteRepository.updateSiteIconMediaId(mediaId.toInt(), true)
    }

    fun handleCropResult(croppedUri: Uri?, success: Boolean) {
        if (success && croppedUri != null) {
            analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_ICON_CROPPED)
            selectedSiteRepository.showSiteIconProgressBar(true)
            uploadIconJob = scope.launch(bgDispatcher) {
                wpMediaUtilsWrapper.fetchMediaToUriWrapper(UriWrapper(croppedUri))?.let { fetchMedia ->
                    mediaUtilsWrapper.getRealPathFromURI(fetchMedia.uri)
                }?.let {
                    startSiteIconUpload(it)
                }
            }
        } else {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_cropping_image)))
            )
        }
    }

    @Suppress("ReturnCount")
    private fun startSiteIconUpload(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_locating_image)))
            )
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.file_error_create))))
            return
        }
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null) {
            val media = buildMediaModel(file, selectedSite)
            if (media == null) {
                _onSnackbarMessage.postValue(
                    Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.file_not_found)))
                )
                return
            }
            _onMediaUpload.postValue(Event(media))
        } else {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_generic))))
        }
    }

    private fun buildMediaModel(file: File, site: SiteModel): MediaModel? {
        val uri = Uri.Builder().path(file.path).build()
        val mimeType = contextProvider.getContext().contentResolver.getType(uri)
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)
    }

    fun onCleared() {
        uploadIconJob?.cancel()
        scope.cancel()
    }
}
