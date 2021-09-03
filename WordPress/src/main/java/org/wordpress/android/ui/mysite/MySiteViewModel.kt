package org.wordpress.android.ui.mysite

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_HIDE_CARD_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_CARD_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.BACKUP
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.DOMAINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.JETPACK_SETTINGS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PEOPLE
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.STATS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.THEMES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.VIEW_SITE
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ShowRemoveNextStepsDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenBackup
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenCropActivity
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomains
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenJetpackSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPeople
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenQuickStartFullScreenDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenUnifiedComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.ShowQuickStartDialog
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationHandler
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Hide
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Pin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Unpin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.SiteItemsBuilder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.BuildConfigWrapper
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
import org.wordpress.android.util.config.OnboardingImprovementsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.UnifiedCommentsListFeatureConfig
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.map
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongMethod")
class MySiteViewModel
@Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteInfoCardBuilder: SiteInfoCardBuilder,
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
    private val scanAndBackupSource: ScanAndBackupSource,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val quickStartItemBuilder: QuickStartItemBuilder,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val quickActionsCardBuilder: QuickActionsCardBuilder,
    private val currentAvatarSource: CurrentAvatarSource,
    private val dynamicCardsSource: DynamicCardsSource,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val unifiedCommentsListFeatureConfig: UnifiedCommentsListFeatureConfig,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val onboardingImprovementsFeatureConfig: OnboardingImprovementsFeatureConfig,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val snackbarSequencer: SnackbarSequencer
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onDynamicCardMenuShown = MutableLiveData<Event<DynamicCardMenuModel>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()
    private val _activeTaskPosition = MutableLiveData<Pair<QuickStartTask, Int>>()

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

    val uiModel: LiveData<UiModel> = MySiteStateProvider(
            viewModelScope,
            selectedSiteRepository,
            quickStartRepository,
            currentAvatarSource,
            domainRegistrationHandler,
            scanAndBackupSource,
            dynamicCardsSource
    ).state.map { (
            currentAvatarUrl,
            site,
            showSiteIconProgressBar,
            isDomainCreditAvailable,
            scanAvailable,
            backupAvailable,
            activeTask,
            quickStartCategories,
            pinnedDynamicCard,
            visibleDynamicCards
    ) ->
        val state = if (site != null) {
            val siteItems = mutableListOf<MySiteCardAndItem>()
            siteItems.add(
                    siteInfoCardBuilder.buildSiteInfoCard(
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
            if (!buildConfigWrapper.isJetpackApp) {
                siteItems.add(
                        quickActionsCardBuilder.build(
                                this::quickActionStatsClick,
                                this::quickActionPagesClick,
                                this::quickActionPostsClick,
                                this::quickActionMediaClick,
                                site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                                activeTask == CHECK_STATS,
                                activeTask == EDIT_HOMEPAGE || activeTask == REVIEW_PAGES
                        )
                )
            }
            if (isDomainCreditAvailable) {
                analyticsTrackerWrapper.track(DOMAIN_CREDIT_PROMPT_SHOWN)
                siteItems.add(DomainRegistrationCard(ListItemInteraction.create(this::domainRegistrationClick)))
            }
            val dynamicCards: Map<DynamicCardType, DynamicCard> = mutableListOf<DynamicCard>().also { list ->
                // Add all possible future dynamic cards here. If we ever have a remote source of dynamic cards, we'd
                // need to implement a smarter solution where we'd build the sources based on the dynamic cards.
                // This means that the stream of dynamic cards would emit a new stream for each of the cards. The
                // current solution is good enough for a few sources.
                if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
                    list.addAll(quickStartCategories.map { category ->
                        quickStartItemBuilder.build(
                                category,
                                pinnedDynamicCard,
                                this::onDynamicCardMoreClick,
                                this::onQuickStartTaskCardClick
                        )
                    })
                }
            }.associateBy { it.dynamicCardType }

            siteItems.addAll(visibleDynamicCards.mapNotNull { dynamicCardType -> dynamicCards[dynamicCardType] })

            if (!quickStartDynamicCardsFeatureConfig.isEnabled()) {
                quickStartCategories.takeIf { it.isNotEmpty() }?.let {
                    siteItems.add(
                            quickStartCardBuilder.build(
                                    quickStartCategories,
                                    this::onQuickStartBlockRemoveMenuItemClick,
                                    this::onQuickStartTaskTypeItemClick
                            )
                    )
                }
            }

            siteItems.addAll(
                    siteItemsBuilder.buildSiteItems(
                            site,
                            this::onItemClick,
                            backupAvailable,
                            scanAvailable,
                            activeTask == QuickStartTask.VIEW_SITE,
                            activeTask == ENABLE_POST_SHARING,
                            activeTask == EXPLORE_PLANS
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
            _activeTaskPosition.postValue(null)
        } else if (_activeTaskPosition.value?.first != quickStartTask && position >= 0) {
            _activeTaskPosition.postValue(quickStartTask to position)
        }
    }

    private fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val navigationAction = when (action) {
                ACTIVITY_LOG -> OpenActivityLog(selectedSite)
                BACKUP -> OpenBackup(selectedSite)
                SCAN -> OpenScan(selectedSite)
                PLAN -> {
                    quickStartRepository.completeTask(EXPLORE_PLANS)
                    OpenPlan(selectedSite)
                }
                POSTS -> OpenPosts(selectedSite)
                PAGES -> {
                    quickStartRepository.completeTask(REVIEW_PAGES)
                    OpenPages(selectedSite)
                }
                ADMIN -> OpenAdmin(selectedSite)
                PEOPLE -> OpenPeople(selectedSite)
                SHARING -> {
                    quickStartRepository.requestNextStepOfTask(ENABLE_POST_SHARING)
                    OpenSharing(selectedSite)
                }
                DOMAINS -> OpenDomains(selectedSite)
                SITE_SETTINGS -> OpenSiteSettings(selectedSite)
                THEMES -> OpenThemes(selectedSite)
                PLUGINS -> OpenPlugins(selectedSite)
                STATS -> {
                    quickStartRepository.completeTask(CHECK_STATS)
                    getStatsNavigationActionForSite(selectedSite)
                }
                MEDIA -> OpenMedia(selectedSite)
                COMMENTS -> {
                    if (unifiedCommentsListFeatureConfig.isEnabled()) {
                        OpenUnifiedComments(selectedSite)
                    } else {
                        OpenComments(selectedSite)
                    }
                }
                VIEW_SITE -> {
                    quickStartRepository.completeTask(QuickStartTask.VIEW_SITE)
                    OpenSite(selectedSite)
                }
                JETPACK_SETTINGS -> OpenJetpackSettings(selectedSite)
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
        _onNavigation.value = Event(OpenQuickStartFullScreenDialog(type, quickStartCardBuilder.getTitle(type)))
    }

    fun onQuickStartTaskCardClick(task: QuickStartTask) {
        quickStartRepository.setActiveTask(task)
    }

    fun onQuickStartFullScreenDialogDismiss() {
        quickStartRepository.refresh()
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
        analyticsTrackerWrapper.track(MY_SITE_ICON_TAPPED)
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
        _onNavigation.value = Event(OpenSite(selectedSite))
    }

    private fun switchSiteClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(OpenSitePicker(selectedSite))
    }

    private fun quickActionStatsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_STATS_TAPPED)
        quickStartRepository.completeTask(CHECK_STATS)
        _onNavigation.value = Event(getStatsNavigationActionForSite(selectedSite))
    }

    private fun quickActionPagesClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_PAGES_TAPPED)
        quickStartRepository.requestNextStepOfTask(EDIT_HOMEPAGE)
        quickStartRepository.completeTask(REVIEW_PAGES)
        _onNavigation.value = Event(OpenPages(selectedSite))
    }

    private fun quickActionPostsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_POSTS_TAPPED)
        _onNavigation.value = Event(OpenPosts(selectedSite))
    }

    private fun quickActionMediaClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(QUICK_ACTION_MEDIA_TAPPED)
        _onNavigation.value = Event(OpenMedia(selectedSite))
    }

    private fun domainRegistrationClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(OpenDomainRegistration(selectedSite))
    }

    fun refresh() {
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        quickStartRepository.refresh()
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
        quickStartRepository.completeTask(UPDATE_SITE_TITLE, true)
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        when (interaction) {
            is Positive -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(UPLOAD_SITE_ICON)
                    _onNavigation.postValue(
                            Event(OpenMediaPicker(requireNotNull(selectedSiteRepository.getSelectedSite())))
                    )
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogPositiveButtonClicked()
            }
            is Negative -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(UPLOAD_SITE_ICON, true)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
                TAG_CHANGE_SITE_ICON_DIALOG -> {
                    analyticsTrackerWrapper.track(MY_SITE_ICON_REMOVED)
                    quickStartRepository.completeTask(UPLOAD_SITE_ICON, true)
                    quickStartRepository.checkAndShowQuickStartNotice()
                    selectedSiteRepository.updateSiteIconMediaId(0, true)
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogNegativeButtonClicked()
            }
            is Dismissed -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(UPLOAD_SITE_ICON, true)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
            }
        }
    }

    private fun onRemoveNextStepsDialogPositiveButtonClicked() {
        analyticsTrackerWrapper.track(QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED)
        quickStartRepository.skipQuickStart()
        refresh()
        clearActiveQuickStartTask()
    }

    private fun onRemoveNextStepsDialogNegativeButtonClicked() {
        analyticsTrackerWrapper.track(QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED)
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
            quickStartRepository.startQuickStart(siteLocalId)
        } else {
            showQuickStartDialog(selectedSiteRepository.getSelectedSite())
        }
    }

    fun onQuickStartMenuInteraction(interaction: DynamicCardMenuInteraction) {
        launch {
            when (interaction) {
                is DynamicCardMenuInteraction.Remove -> {
                    analyticsTrackerWrapper.track(QUICK_START_REMOVE_CARD_TAPPED)
                    dynamicCardsSource.removeItem(interaction.cardType)
                    quickStartRepository.refresh()
                }
                is Pin -> dynamicCardsSource.pinItem(interaction.cardType)
                is Unpin -> dynamicCardsSource.unpinItem()
                is Hide -> {
                    analyticsTrackerWrapper.track(QUICK_START_HIDE_CARD_TAPPED)
                    dynamicCardsSource.hideItem(interaction.cardType)
                    quickStartRepository.refresh()
                }
            }
        }
    }

    private fun showQuickStartDialog(siteModel: SiteModel?) {
        if (siteModel != null && quickStartUtilsWrapper.isQuickStartAvailableForTheSite(siteModel)) {
            if (onboardingImprovementsFeatureConfig.isEnabled()) {
                _onNavigation.postValue(
                        Event(
                                ShowQuickStartDialog(
                                        R.string.quick_start_dialog_need_help_manage_site_title,
                                        R.string.quick_start_dialog_need_help_manage_site_message,
                                        R.string.quick_start_dialog_need_help_manage_site_button_positive,
                                        R.string.quick_start_dialog_need_help_button_negative
                                )
                        )
                )
            } else {
                if (appPrefsWrapper.isQuickStartEnabled()) {
                    _onNavigation.postValue(
                            Event(
                                    ShowQuickStartDialog(
                                            R.string.quick_start_dialog_need_help_title,
                                            R.string.quick_start_dialog_need_help_message,
                                            R.string.quick_start_dialog_need_help_button_positive,
                                            R.string.quick_start_dialog_need_help_manage_site_button_negative,
                                            R.string.quick_start_dialog_need_help_button_neutral
                                    )
                            )
                    )
                }
            }
        }
    }

    fun startQuickStart() {
        analyticsTrackerWrapper.track(QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
        quickStartRepository.startQuickStart(selectedSiteRepository.getSelectedSiteLocalId())
    }

    fun ignoreQuickStart() {
        analyticsTrackerWrapper.track(QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    fun disableQuickStart() {
        if (!onboardingImprovementsFeatureConfig.isEnabled()) {
            analyticsTrackerWrapper.track(QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED)
            appPrefsWrapper.setQuickStartDisabled(true)
        }
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

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
    }
}
