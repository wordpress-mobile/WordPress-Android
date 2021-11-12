package org.wordpress.android.ui.mysite

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ShowRemoveNextStepsDialog
import org.wordpress.android.ui.mysite.cards.CardsBuilder
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.post.PostCardsSource
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Hide
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Pin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Unpin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsBuilder
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.ui.mysite.items.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.UnifiedCommentsListFeatureConfig
import org.wordpress.android.util.filter
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.map
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongMethod", "LongParameterList", "TooManyFunctions")
class MySiteViewModel @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val siteStoriesHandler: SiteStoriesHandler,
    private val domainRegistrationSource: DomainRegistrationSource,
    private val scanAndBackupSource: ScanAndBackupSource,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val quickStartCardSource: QuickStartCardSource,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val currentAvatarSource: CurrentAvatarSource,
    private val dynamicCardsSource: DynamicCardsSource,
    private val unifiedCommentsListFeatureConfig: UnifiedCommentsListFeatureConfig,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val snackbarSequencer: SnackbarSequencer,
    private val cardsBuilder: CardsBuilder,
    private val dynamicCardsBuilder: DynamicCardsBuilder,
    private val postCardsSource: PostCardsSource,
    selectedSiteSource: SelectedSiteSource,
    siteIconProgressSource: SiteIconProgressSource,
    mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onDynamicCardMenuShown = MutableLiveData<Event<DynamicCardMenuModel>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()
    private val _activeTaskPosition = MutableLiveData<Pair<QuickStartTask, Int>>()
    private val _onShowSwipeRefreshLayout = MutableLiveData((Event(mySiteDashboardPhase2FeatureConfig.isEnabled())))

    val onScrollTo: LiveData<Event<Int>> = merge(
            _activeTaskPosition.distinctUntilChanged(),
            quickStartRepository.activeTask
    ) { pair, activeTask ->
        if (pair != null && activeTask != null && pair.first == activeTask) {
            Event(pair.second)
        } else {
            null
        }
    }
    val onSnackbarMessage = merge(_onSnackbarMessage, siteStoriesHandler.onSnackbar, quickStartRepository.onSnackbar)
    val onQuickStartMySitePrompts = quickStartRepository.onQuickStartMySitePrompts
    val onTextInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<SiteDialogModel>>
    val onDynamicCardMenuShown = _onDynamicCardMenuShown as LiveData<Event<DynamicCardMenuModel>>
    val onNavigation = merge(_onNavigation, siteStoriesHandler.onNavigation)
    val onMediaUpload = _onMediaUpload as LiveData<Event<MediaModel>>
    val onUploadedItem = siteIconUploadHandler.onUploadedItem
    val onShowSwipeRefreshLayout = _onShowSwipeRefreshLayout

    private val mySiteSources: List<MySiteSource<*>> = listOf(
            selectedSiteSource,
            siteIconProgressSource,
            quickStartCardSource,
            currentAvatarSource,
            domainRegistrationSource,
            scanAndBackupSource,
            dynamicCardsSource,
            postCardsSource
    )

    val state: LiveData<MySiteUiState> = selectedSiteRepository.siteSelected.switchMap { siteLocalId ->
        val result = MediatorLiveData<SiteIdToState>()
        val currentSources = if (siteLocalId != null) {
            mySiteSources.map { source -> source.buildSource(viewModelScope, siteLocalId).distinctUntilChanged() }
        } else {
            mySiteSources.filterIsInstance(SiteIndependentSource::class.java)
                    .map { source -> source.buildSource(viewModelScope).distinctUntilChanged() }
        }
        for (newSource in currentSources) {
            result.addSource(newSource) { partialState ->
                if (partialState != null) {
                    result.value = (result.value ?: SiteIdToState(siteLocalId)).update(partialState)
                }
            }
        }
        // We want to filter out the empty state where we have a site ID but site object is missing.
        // Without this check there is an emission of a NoSites state even if we have the site
        result.filter { it.siteId == null || it.state.site != null }.map { it.state }
    }.distinctUntilChanged()

    val uiModel: LiveData<UiModel> = state.map { (
            currentAvatarUrl,
            site,
            showSiteIconProgressBar,
            isDomainCreditAvailable,
            scanAvailable,
            backupAvailable,
            activeTask,
            quickStartCategories,
            pinnedDynamicCard,
            visibleDynamicCards,
            mockedPostData
    ) ->
        val state = if (site != null) {
            buildSiteSelectedStateAndScroll(
                    site,
                    showSiteIconProgressBar,
                    activeTask,
                    isDomainCreditAvailable,
                    quickStartCategories,
                    pinnedDynamicCard,
                    visibleDynamicCards,
                    backupAvailable,
                    scanAvailable,
                    mockedPostData
            )
        } else {
            buildNoSiteState()
        }
        UiModel(currentAvatarUrl.orEmpty(), state)
    }

    @Suppress("LongParameterList")
    private fun buildSiteSelectedStateAndScroll(
        site: SiteModel,
        showSiteIconProgressBar: Boolean,
        activeTask: QuickStartTask?,
        isDomainCreditAvailable: Boolean,
        quickStartCategories: List<QuickStartCategory>,
        pinnedDynamicCard: DynamicCardType?,
        visibleDynamicCards: List<DynamicCardType>,
        backupAvailable: Boolean,
        scanAvailable: Boolean,
        mockedPostsData: MockedPostsData?
    ): SiteSelected {
        val siteItems = buildSiteSelectedState(
                site,
                showSiteIconProgressBar,
                activeTask,
                isDomainCreditAvailable,
                quickStartCategories,
                pinnedDynamicCard,
                visibleDynamicCards,
                backupAvailable,
                scanAvailable,
                mockedPostsData
        )
        scrollToQuickStartTaskIfNecessary(
                activeTask,
                siteItems.indexOfFirst { it.activeQuickStartItem }
        )
        return SiteSelected(siteItems)
    }

    @Suppress("LongParameterList")
    private fun buildSiteSelectedState(
        site: SiteModel,
        showSiteIconProgressBar: Boolean,
        activeTask: QuickStartTask?,
        isDomainCreditAvailable: Boolean,
        quickStartCategories: List<QuickStartCategory>,
        pinnedDynamicCard: DynamicCardType?,
        visibleDynamicCards: List<DynamicCardType>,
        backupAvailable: Boolean,
        scanAvailable: Boolean,
        mockedPostsData: MockedPostsData?
    ) = cardsBuilder.build(
            DomainRegistrationCardBuilderParams(
                    isDomainCreditAvailable = isDomainCreditAvailable,
                    domainRegistrationClick = this::domainRegistrationClick
            ),
            PostCardBuilderParams(mockedPostsData = mockedPostsData),
            QuickActionsCardBuilderParams(
                    siteModel = site,
                    activeTask = activeTask,
                    onQuickActionStatsClick = this::quickActionStatsClick,
                    onQuickActionPagesClick = this::quickActionPagesClick,
                    onQuickActionPostsClick = this::quickActionPostsClick,
                    onQuickActionMediaClick = this::quickActionMediaClick
            ),
            QuickStartCardBuilderParams(
                    quickStartCategories = quickStartCategories,
                    onQuickStartBlockRemoveMenuItemClick = this::onQuickStartBlockRemoveMenuItemClick,
                    onQuickStartTaskTypeItemClick = this::onQuickStartTaskTypeItemClick
            ),
            SiteInfoCardBuilderParams(
                    site = site,
                    showSiteIconProgressBar = showSiteIconProgressBar,
                    titleClick = this::titleClick,
                    iconClick = this::iconClick,
                    urlClick = this::urlClick,
                    switchSiteClick = this::switchSiteClick,
                    activeTask = activeTask
            )
    ) + dynamicCardsBuilder.build(
            quickStartCategories,
            pinnedDynamicCard,
            visibleDynamicCards,
            this::onDynamicCardMoreClick,
            this::onQuickStartTaskCardClick
    ) + siteItemsBuilder.build(
            SiteItemsBuilderParams(
                    site = site,
                    activeTask = activeTask,
                    backupAvailable = backupAvailable,
                    scanAvailable = scanAvailable,
                    onClick = this::onItemClick
            )
    )

    private fun buildNoSiteState(): NoSites {
        // Hide actionable empty view image when screen height is under specified min height.
        val shouldShowImage = displayUtilsWrapper.getDisplayPixelHeight() >= MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE
        return NoSites(shouldShowImage)
    }

    private fun scrollToQuickStartTaskIfNecessary(
        quickStartTask: QuickStartTask?,
        position: Int
    ) {
        if (quickStartTask == null) {
            _activeTaskPosition.postValue(null)
        } else if (_activeTaskPosition.value?.first != quickStartTask && position >= 0) {
            _activeTaskPosition.postValue(quickStartTask to position)
        }
    }

    @Suppress("ComplexMethod")
    private fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val navigationAction = when (action) {
                ListItemAction.ACTIVITY_LOG -> SiteNavigationAction.OpenActivityLog(selectedSite)
                ListItemAction.BACKUP -> SiteNavigationAction.OpenBackup(selectedSite)
                ListItemAction.SCAN -> SiteNavigationAction.OpenScan(selectedSite)
                ListItemAction.PLAN -> {
                    quickStartRepository.completeTask(QuickStartTask.EXPLORE_PLANS)
                    SiteNavigationAction.OpenPlan(selectedSite)
                }
                ListItemAction.POSTS -> SiteNavigationAction.OpenPosts(selectedSite)
                ListItemAction.PAGES -> {
                    quickStartRepository.completeTask(QuickStartTask.REVIEW_PAGES)
                    SiteNavigationAction.OpenPages(selectedSite)
                }
                ListItemAction.ADMIN -> SiteNavigationAction.OpenAdmin(selectedSite)
                ListItemAction.PEOPLE -> SiteNavigationAction.OpenPeople(selectedSite)
                ListItemAction.SHARING -> {
                    quickStartRepository.requestNextStepOfTask(QuickStartTask.ENABLE_POST_SHARING)
                    SiteNavigationAction.OpenSharing(selectedSite)
                }
                ListItemAction.DOMAINS -> SiteNavigationAction.OpenDomains(selectedSite)
                ListItemAction.SITE_SETTINGS -> SiteNavigationAction.OpenSiteSettings(selectedSite)
                ListItemAction.THEMES -> SiteNavigationAction.OpenThemes(selectedSite)
                ListItemAction.PLUGINS -> SiteNavigationAction.OpenPlugins(selectedSite)
                ListItemAction.STATS -> {
                    quickStartRepository.completeTask(QuickStartTask.CHECK_STATS)
                    getStatsNavigationActionForSite(selectedSite)
                }
                ListItemAction.MEDIA -> SiteNavigationAction.OpenMedia(selectedSite)
                ListItemAction.COMMENTS -> {
                    if (unifiedCommentsListFeatureConfig.isEnabled()) {
                        SiteNavigationAction.OpenUnifiedComments(selectedSite)
                    } else {
                        SiteNavigationAction.OpenComments(selectedSite)
                    }
                }
                ListItemAction.VIEW_SITE -> {
                    quickStartRepository.completeTask(QuickStartTask.VIEW_SITE)
                    SiteNavigationAction.OpenSite(selectedSite)
                }
                ListItemAction.JETPACK_SETTINGS -> SiteNavigationAction.OpenJetpackSettings(selectedSite)
            }
            _onNavigation.postValue(Event(navigationAction))
        } ?: _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.site_cannot_be_loaded))))
    }

    private fun onDynamicCardMoreClick(model: DynamicCardMenuModel) {
        _onDynamicCardMenuShown.postValue(Event(model))
    }

    private fun onQuickStartBlockRemoveMenuItemClick() {
        _onBasicDialogShown.value = Event(ShowRemoveNextStepsDialog)
    }

    private fun onQuickStartTaskTypeItemClick(type: QuickStartTaskType) {
        clearActiveQuickStartTask()
        _onNavigation.value = Event(
                SiteNavigationAction.OpenQuickStartFullScreenDialog(type, quickStartCardBuilder.getTitle(type))
        )
    }

    fun onQuickStartTaskCardClick(task: QuickStartTask) {
        quickStartRepository.setActiveTask(task)
    }

    fun onQuickStartFullScreenDialogDismiss() {
        quickStartCardSource.refresh()
    }

    private fun titleClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
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
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_TAPPED)
        val hasIcon = selectedSite.iconUrl != null
        if (selectedSite.hasCapabilityManageOptions && selectedSite.hasCapabilityUploadFiles) {
            if (hasIcon) {
                _onBasicDialogShown.value = Event(ChangeSiteIconDialogModel)
            } else {
                _onBasicDialogShown.value = Event(AddSiteIconDialogModel)
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
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(message)))
        }
    }

    private fun urlClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(SiteNavigationAction.OpenSite(selectedSite))
    }

    private fun switchSiteClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(SiteNavigationAction.OpenSitePicker(selectedSite))
    }

    private fun quickActionStatsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.QUICK_ACTION_STATS_TAPPED)
        quickStartRepository.completeTask(QuickStartTask.CHECK_STATS)
        _onNavigation.value = Event(getStatsNavigationActionForSite(selectedSite))
    }

    private fun quickActionPagesClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.QUICK_ACTION_PAGES_TAPPED)
        quickStartRepository.requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
        quickStartRepository.completeTask(QuickStartTask.REVIEW_PAGES)
        _onNavigation.value = Event(SiteNavigationAction.OpenPages(selectedSite))
    }

    private fun quickActionPostsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.QUICK_ACTION_POSTS_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenPosts(selectedSite))
    }

    private fun quickActionMediaClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.QUICK_ACTION_MEDIA_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenMedia(selectedSite))
    }

    private fun domainRegistrationClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(SiteNavigationAction.OpenDomainRegistration(selectedSite))
    }

    fun refresh() {
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        quickStartCardSource.refresh()
        currentAvatarSource.refresh()
    }

    fun clearActiveQuickStartTask() {
        quickStartRepository.clearActiveTask()
    }

    fun checkAndShowQuickStartNotice() {
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun dismissQuickStartNotice() {
        if (quickStartRepository.isQuickStartNoticeShown) snackbarSequencer.dismissLastSnackbar()
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
        // This callback is called even when the dialog interaction is positive,
        // otherwise we would need to call 'completeTask' on 'onSiteNameChosen' as well.
        quickStartRepository.completeTask(QuickStartTask.UPDATE_SITE_TITLE)
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        when (interaction) {
            is Positive -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    _onNavigation.postValue(
                            Event(
                                    SiteNavigationAction.OpenMediaPicker(
                                            requireNotNull(selectedSiteRepository.getSelectedSite())
                                    )
                            )
                    )
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogPositiveButtonClicked()
            }
            is Negative -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
                TAG_CHANGE_SITE_ICON_DIALOG -> {
                    analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_REMOVED)
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                    selectedSiteRepository.updateSiteIconMediaId(0, true)
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogNegativeButtonClicked()
            }
            is Dismissed -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
            }
        }
    }

    private fun onRemoveNextStepsDialogPositiveButtonClicked() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED)
        quickStartRepository.skipQuickStart()
        refresh()
        clearActiveQuickStartTask()
    }

    private fun onRemoveNextStepsDialogNegativeButtonClicked() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED)
    }

    fun handleTakenSiteIcon(iconUrl: String?, source: PhotoPickerMediaSource?) {
        val stat = if (source == ANDROID_CAMERA) Stat.MY_SITE_ICON_SHOT_NEW else Stat.MY_SITE_ICON_GALLERY_PICKED
        analyticsTrackerWrapper.track(stat)
        val imageUri = Uri.parse(iconUrl)?.let { UriWrapper(it) }
        if (imageUri != null) {
            selectedSiteRepository.showSiteIconProgressBar(true)
            launch(bgDispatcher) {
                val fetchMedia = wpMediaUtilsWrapper.fetchMediaToUriWrapper(imageUri)
                if (fetchMedia != null) {
                    _onNavigation.postValue(Event(SiteNavigationAction.OpenCropActivity(fetchMedia)))
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
            analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_CROPPED)
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
        selectedSiteRepository.getSelectedSite()?.let { site ->
            _onNavigation.value = Event(
                    SiteNavigationAction.OpenStats(site)
            )
        }
    }

    fun handleSuccessfulDomainRegistrationResult(email: String?) {
        analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)
        _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(getEmailValidationMessage(email))))
    }

    @Suppress("ReturnCount")
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
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null) {
            val media = buildMediaModel(file, selectedSite)
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
        !accountStore.hasAccessToken() && site.isJetpackConnected -> SiteNavigationAction.StartWPComLoginForJetpackStats

        // If it's a WordPress.com or Jetpack site, show the Stats screen.
        site.isWPCom || site.isJetpackInstalled && site.isJetpackConnected -> SiteNavigationAction.OpenStats(site)

        // If it's a self-hosted site, ask to connect to Jetpack.
        else -> SiteNavigationAction.ConnectJetpackForStats(site)
    }

    fun onAvatarPressed() {
        _onNavigation.value = Event(SiteNavigationAction.OpenMeScreen)
    }

    fun onAddSitePressed() {
        _onNavigation.value = Event(SiteNavigationAction.AddNewSite(accountStore.hasAccessToken()))
    }

    override fun onCleared() {
        siteIconUploadHandler.clear()
        siteStoriesHandler.clear()
        domainRegistrationSource.clear()
        quickStartRepository.clear()
        scanAndBackupSource.clear()
        super.onCleared()
    }

    fun handleStoriesPhotoPickerResult(data: Intent) {
        selectedSiteRepository.getSelectedSite()?.let {
            siteStoriesHandler.handleStoriesResult(it, data, STORY_FROM_MY_SITE)
        }
    }

    fun checkAndStartQuickStart(siteLocalId: Int) {
        if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            startQuickStart(siteLocalId)
        } else {
            showQuickStartDialog(selectedSiteRepository.getSelectedSite())
        }
    }

    private fun startQuickStart(siteLocalId: Int) {
        if (siteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            quickStartUtilsWrapper.startQuickStart(siteLocalId)
            quickStartCardSource.refresh()
        }
    }

    fun onQuickStartMenuInteraction(interaction: DynamicCardMenuInteraction) {
        launch {
            when (interaction) {
                is DynamicCardMenuInteraction.Remove -> {
                    analyticsTrackerWrapper.track(Stat.QUICK_START_REMOVE_CARD_TAPPED)
                    dynamicCardsSource.removeItem(interaction.cardType)
                    quickStartCardSource.refresh()
                }
                is Pin -> dynamicCardsSource.pinItem(interaction.cardType)
                is Unpin -> dynamicCardsSource.unpinItem()
                is Hide -> {
                    analyticsTrackerWrapper.track(Stat.QUICK_START_HIDE_CARD_TAPPED)
                    dynamicCardsSource.hideItem(interaction.cardType)
                    quickStartCardSource.refresh()
                }
            }
        }
    }

    private fun showQuickStartDialog(siteModel: SiteModel?) {
        if (siteModel != null && quickStartUtilsWrapper.isQuickStartAvailableForTheSite(siteModel)) {
            _onNavigation.postValue(
                    Event(
                            SiteNavigationAction.ShowQuickStartDialog(
                                    R.string.quick_start_dialog_need_help_manage_site_title,
                                    R.string.quick_start_dialog_need_help_manage_site_message,
                                    R.string.quick_start_dialog_need_help_manage_site_button_positive,
                                    R.string.quick_start_dialog_need_help_button_negative
                            )
                    )
            )
        }
    }

    fun startQuickStart() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
        startQuickStart(selectedSiteRepository.getSelectedSiteLocalId())
    }

    fun ignoreQuickStart() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    fun onPullToRefresh() {
        postCardsSource.refresh()
    }

    data class UiModel(
        val accountAvatarUrl: String,
        val state: State
    )

    sealed class State {
        data class SiteSelected(val cardAndItems: List<MySiteCardAndItem>) : State()
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

    private data class SiteIdToState(val siteId: Int?, val state: MySiteUiState = MySiteUiState()) {
        fun update(partialState: PartialState): SiteIdToState {
            return this.copy(state = state.update(partialState))
        }
    }

    companion object {
        private const val MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE = 600
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
    }
}
