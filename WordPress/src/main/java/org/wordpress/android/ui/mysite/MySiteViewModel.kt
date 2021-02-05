package org.wordpress.android.ui.mysite

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_PROMPT_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_CROPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_GALLERY_PICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_REMOVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_SHOT_NEW
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_MEDIA_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_PAGES_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_POSTS_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_ACTION_STATS_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_CARD_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_HIDE_CARD_TAPPED
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.mysite.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.ListItemAction.BACKUP
import org.wordpress.android.ui.mysite.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.ListItemAction.JETPACK_SETTINGS
import org.wordpress.android.ui.mysite.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.ListItemAction.PEOPLE
import org.wordpress.android.ui.mysite.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.ListItemAction.STATS
import org.wordpress.android.ui.mysite.ListItemAction.THEMES
import org.wordpress.android.ui.mysite.ListItemAction.VIEW_SITE
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Hide
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Pin
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenBackup
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenCropActivity
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenJetpackSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPeople
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.map
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
    private val siteItemsBuilder: SiteItemsBuilder,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val siteStoriesHandler: SiteStoriesHandler,
    private val domainRegistrationHandler: DomainRegistrationHandler,
    scanAndBackupSource: ScanAndBackupSource,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val quickStartItemBuilder: QuickStartItemBuilder,
    private val currentAvatarSource: CurrentAvatarSource
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onQuickStartMenuShown = MutableLiveData<Event<String>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()
    private val _scrollToQuickStartTask = MutableLiveData<Event<Pair<QuickStartTask, Int>>>()

    val onScrollTo: LiveData<Event<Pair<QuickStartTask, Int>>> = _scrollToQuickStartTask
    val onSnackbarMessage = merge(_onSnackbarMessage, siteStoriesHandler.onSnackbar, quickStartRepository.onSnackbar)
    val onTextInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<SiteDialogModel>>
    val onQuickStartMenuShown = _onQuickStartMenuShown as LiveData<Event<String>>
    val onNavigation = merge(_onNavigation, siteStoriesHandler.onNavigation)
    val onMediaUpload = _onMediaUpload as LiveData<Event<MediaModel>>
    val onUploadedItem = siteIconUploadHandler.onUploadedItem

    val uiModel: LiveData<UiModel> = MySiteStateProvider(
            bgDispatcher,
            selectedSiteRepository,
            quickStartRepository,
            currentAvatarSource,
            domainRegistrationHandler,
            scanAndBackupSource
    ).state.map { (
            currentAvatarUrl,
            site,
            showSiteIconProgressBar,
            isDomainCreditAvailable,
            scanAvailable,
            backupAvailable,
            activeTask,
            quickStartCategories
    ) ->
        val state = if (site != null) {
            val siteItems = mutableListOf<MySiteItem>()
            siteItems.add(
                    siteInfoBlockBuilder.buildSiteInfoBlock(
                            site,
                            showSiteIconProgressBar,
                            this::titleClick,
                            this::iconClick,
                            this::urlClick,
                            this::switchSiteClick,
                            activeTask == UPDATE_SITE_TITLE,
                            activeTask == UPLOAD_SITE_ICON
                    )
            )
            siteItems.add(
                    QuickActionsBlock(
                            ListItemInteraction.create(this::quickActionStatsClick),
                            ListItemInteraction.create(this::quickActionPagesClick),
                            ListItemInteraction.create(this::quickActionPostsClick),
                            ListItemInteraction.create(this::quickActionMediaClick),
                            site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                            activeTask == CHECK_STATS,
                            activeTask == EDIT_HOMEPAGE || activeTask == REVIEW_PAGES
                    )
            )
            if (isDomainCreditAvailable) {
                analyticsTrackerWrapper.track(DOMAIN_CREDIT_PROMPT_SHOWN)
                siteItems.add(DomainRegistrationBlock(ListItemInteraction.create(this::domainRegistrationClick)))
            }

            siteItems.addAll(quickStartCategories.map {
                quickStartItemBuilder.build(
                        it,
                        this::onQuickStartCardMoreClick,
                        this::onQuickStartTaskCardClick
                )
            })

            siteItems.addAll(
                    siteItemsBuilder.buildSiteItems(
                            site,
                            this::onItemClick,
                            backupAvailable,
                            scanAvailable,
                            activeTask == QuickStartTask.VIEW_SITE,
                            activeTask == ENABLE_POST_SHARING
                    )
            )
            scrollToQuickStartTaskIfNecessary(
                    activeTask,
                    siteItems.indexOfFirst { it.activeQuickStartItem })
            State.SiteSelected(siteItems)
        } else {
            // Hide actionable empty view image when screen height is under 600 pixels.
            val shouldShowImage = displayUtilsWrapper.getDisplayPixelHeight() >= 600
            State.NoSites(shouldShowImage)
        }
        UiModel(currentAvatarUrl.orEmpty(), state)
    }

    private fun scrollToQuickStartTaskIfNecessary(
        quickStartTask: QuickStartTask?,
        position: Int
    ) {
        if (quickStartTask == null) {
            _scrollToQuickStartTask.postValue(null)
        } else if (_scrollToQuickStartTask.value?.peekContent()?.first != quickStartTask && position >= 0) {
            _scrollToQuickStartTask.postValue(Event(quickStartTask to position))
        }
    }

    private fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            val navigationAction = when (action) {
                ACTIVITY_LOG -> OpenActivityLog(site)
                BACKUP -> OpenBackup(site)
                SCAN -> OpenScan(site)
                PLAN -> OpenPlan(site)
                POSTS -> OpenPosts(site)
                PAGES -> {
                    quickStartRepository.completeTask(REVIEW_PAGES)
                    OpenPages(site)
                }
                ADMIN -> OpenAdmin(site)
                PEOPLE -> OpenPeople(site)
                SHARING -> {
                    quickStartRepository.requestNextStepOfTask(ENABLE_POST_SHARING)
                    OpenSharing(site)
                }
                SITE_SETTINGS -> OpenSiteSettings(site)
                THEMES -> OpenThemes(site)
                PLUGINS -> OpenPlugins(site)
                STATS -> {
                    quickStartRepository.completeTask(CHECK_STATS)
                    getStatsNavigationActionForSite(site)
                }
                MEDIA -> OpenMedia(site)
                COMMENTS -> OpenComments(site)
                VIEW_SITE -> {
                    quickStartRepository.completeTask(QuickStartTask.VIEW_SITE)
                    OpenSite(site)
                }
                JETPACK_SETTINGS -> OpenJetpackSettings(site)
            }
            _onNavigation.postValue(Event(navigationAction))
        } ?: _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.site_cannot_be_loaded))))
    }

    private fun onQuickStartCardMoreClick(id: String) {
        _onQuickStartMenuShown.postValue(Event(id))
    }

    private fun onQuickStartTaskCardClick(task: QuickStartTask) {
        quickStartRepository.setActiveTask(task)
    }

    private fun titleClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        quickStartRepository.completeTask(UPDATE_SITE_TITLE)
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

    private fun iconClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(MY_SITE_ICON_TAPPED)
        quickStartRepository.completeTask(UPLOAD_SITE_ICON)
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

    private fun urlClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(OpenSite(site))
    }

    private fun switchSiteClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(OpenSitePicker(site))
    }

    private fun quickActionStatsClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_STATS_TAPPED)
        quickStartRepository.completeTask(CHECK_STATS)
        _onNavigation.value = Event(getStatsNavigationActionForSite(site))
    }

    private fun quickActionPagesClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_PAGES_TAPPED)
        quickStartRepository.requestNextStepOfTask(EDIT_HOMEPAGE)
        quickStartRepository.completeTask(REVIEW_PAGES)
        _onNavigation.value = Event(OpenPages(site))
    }

    private fun quickActionPostsClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_POSTS_TAPPED)
        _onNavigation.value = Event(OpenPosts(site))
    }

    private fun quickActionMediaClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_MEDIA_TAPPED)
        _onNavigation.value = Event(OpenMedia(site))
    }

    private fun domainRegistrationClick() {
        val site = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(OpenDomainRegistration(site))
    }

    fun refresh() {
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        quickStartRepository.refresh()
        currentAvatarSource.refresh()
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

    fun handleSelectedSiteIcon(mediaId: Long) {
        selectedSiteRepository.updateSiteIconMediaId(mediaId.toInt(), true)
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
        selectedSiteRepository.getSelectedSite()?.let { site -> _onNavigation.value = Event(OpenStats(site)) }
    }

    fun handleSuccessfulDomainRegistrationResult(email: String?) {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)
        _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(getEmailValidationMessage(email))))
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

    private fun getStatsNavigationActionForSite(site: SiteModel) = when {
        // If the user is not logged in and the site is already connected to Jetpack, ask to login.
        !accountStore.hasAccessToken() && site.isJetpackConnected -> StartWPComLoginForJetpackStats

        // If it's a WordPress.com or Jetpack site, show the Stats screen.
        site.isWPCom || site.isJetpackInstalled && site.isJetpackConnected -> OpenStats(site)

        // If it's a self-hosted site, ask to connect to Jetpack.
        else -> ConnectJetpackForStats(site)
    }

    fun onAvatarPressed() {
        _onNavigation.value = Event(OpenMeScreen)
    }

    fun onAddSitePressed() {
        _onNavigation.value = Event(AddNewSite(accountStore.hasAccessToken()))
    }

    override fun onCleared() {
        siteIconUploadHandler.clear()
        siteStoriesHandler.clear()
        domainRegistrationHandler.clear()
        quickStartRepository.clear()
        super.onCleared()
    }

    fun handleStoriesPhotoPickerResult(data: Intent) {
        selectedSiteRepository.getSelectedSite()?.let {
            siteStoriesHandler.handleStoriesResult(it, data, STORY_FROM_MY_SITE)
        }
    }

    fun startQuickStart() {
        quickStartRepository.startQuickStart()
    }

    fun onQuickStartMenuInteraction(interaction: QuickStartMenuInteraction) {
        when (interaction) {
            is QuickStartMenuInteraction.Remove -> {
                analyticsTrackerWrapper.track(QUICK_START_REMOVE_CARD_TAPPED)
                quickStartRepository.removeCategory(interaction.id)
            }
            is Pin -> TODO()
            is Hide -> {
                analyticsTrackerWrapper.track(QUICK_START_HIDE_CARD_TAPPED)
                quickStartRepository.hideCategory(interaction.id)
            }
        }
    }

    data class UiModel(
        val accountAvatarUrl: String,
        val state: State
    )

    sealed class State {
        data class SiteSelected(val items: List<MySiteItem>) : State()
        data class NoSites(val shouldShowImage: Boolean) : State()
    }

    data class TextInputDialogModel(
        val callbackId: Int = SITE_NAME_CHANGE_CALLBACK_ID,
        @StringRes val title: Int,
        val initialText: String,
        @StringRes val hint: Int,
        val isMultiline: Boolean,
        val isInputEnabled: Boolean
    )

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
    }
}
