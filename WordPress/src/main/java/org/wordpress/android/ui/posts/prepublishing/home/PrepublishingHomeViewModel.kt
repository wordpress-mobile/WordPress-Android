package org.wordpress.android.ui.posts.prepublishing.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.FeaturedImageHelper
import org.wordpress.android.ui.posts.FeaturedImageHelper.TrackableEvent
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.posts.UpdateFeaturedImageUseCase
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.PrepublishingScreenNavigation
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.FeaturedImageUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetButtonUiStateUseCase
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val postSettingsUtils: PostSettingsUtils,
    private val getButtonUiStateUseCase: GetButtonUiStateUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val featuredImageHelper: FeaturedImageHelper,
    private val updateFeaturedImageUseCase: UpdateFeaturedImageUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var updateStoryTitleJob: Job? = null
    private var refreshFeaturedImageJob: Job? = null
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var site: SiteModel
    private var isFeaturedImageEditingSupported = false

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    private val _onSubmitButtonClicked = MutableLiveData<Event<PublishPost>>()
    val onSubmitButtonClicked: LiveData<Event<PublishPost>> = _onSubmitButtonClicked

    // The publish/submit button is pinned as a footer outside the scrolling list, so it's exposed
    // separately rather than as a list item.
    private val _buttonUiState = MutableLiveData<ButtonUiState>()
    val buttonUiState: LiveData<ButtonUiState> = _buttonUiState

    private val _launchFeaturedImagePicker = MutableLiveData<Event<Int>>()
    val launchFeaturedImagePicker: LiveData<Event<Int>> = _launchFeaturedImagePicker

    private val _syncFeaturedImageToEditor = MutableLiveData<Event<Unit>>()
    val syncFeaturedImageToEditor: LiveData<Event<Unit>> = _syncFeaturedImageToEditor

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    private var _socialUiState: MutableLiveData<SocialUiState> = MutableLiveData(SocialUiState.Hidden)
    private val _featuredImageUiState: MutableLiveData<FeaturedImageUiState> =
        MutableLiveData(FeaturedImageUiState.Hidden)

    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = merge(
        _uiState,
        _socialUiState,
        _featuredImageUiState
    ) { list, socialUiState, featuredImageUiState ->
        list?.map { item ->
            when (item) {
                is SocialUiState -> socialUiState ?: SocialUiState.Hidden
                is FeaturedImageUiState -> featuredImageUiState ?: FeaturedImageUiState.Hidden
                else -> item
            }
        }
    }

    fun start(
        editPostRepository: EditPostRepository,
        site: SiteModel,
        isFeaturedImageEditingSupported: Boolean = true
    ) {
        this.editPostRepository = editPostRepository
        this.site = site
        this.isFeaturedImageEditingSupported = isFeaturedImageEditingSupported
        if (isStarted) return
        // PrepublishingViewModel already dismisses the sheet when the host has no post, but that
        // dismissal is asynchronous and the FragmentManager can restore this fragment and create its
        // view before it commits. Skip the UI rather than reading a post that isn't there.
        if (!editPostRepository.hasPost()) {
            AppLog.e(AppLog.T.POSTS, "Prepublishing home started without a post; skipping setup.")
            return
        }
        isStarted = true

        setupHomeUiState(editPostRepository, site)
    }

    private fun setupHomeUiState(
        editPostRepository: EditPostRepository,
        site: SiteModel
    ) {
        // Built synchronously alongside the other rows so the card is present in the first frame instead
        // of popping in (and reflowing the sheet) once a background refresh completes. Later updates go
        // through refreshFeaturedImage, which runs off the main thread.
        val featuredImageUiState = createFeaturedImageUiState()
        val prepublishingHomeUiStateList = mutableListOf<PrepublishingHomeItemUiState>().apply {
            add(
                HeaderUiState(
                    UiStringText(site.name),
                    StringUtils.notNullStr(site.iconUrl)
                )
            )

            if (editPostRepository.status != PostStatus.PRIVATE) {
                showPublicPost(editPostRepository)
            } else {
                showPrivatePost(editPostRepository)
            }

            if (!editPostRepository.isPage) {
                showNotSetPost(editPostRepository, site)
            } else {
                UiStringRes(R.string.prepublishing_nudges_home_categories_not_set)
            }

            val categoriesString = getCategoriesUseCase.getPostCategoriesString(
                editPostRepository,
                site
            )

            if (!editPostRepository.isPage) {
                add(HomeUiState(
                    navigationAction = PrepublishingScreenNavigation.Categories,
                    actionResult = if (categoriesString.isNotEmpty()) {
                        UiStringText(categoriesString)
                    } else {
                        run { UiStringRes(R.string.prepublishing_nudges_home_categories_not_set) }
                    },
                    actionClickable = true,
                    onNavigationActionClicked = ::onActionClicked
                ))
            }

            // Replaced via merge with the live featured-image state; it stays Hidden for pages /
            // unsupported hosts.
            add(featuredImageUiState)

            add(SocialUiState.Hidden)
        }.toList()

        _featuredImageUiState.postValue(featuredImageUiState)
        _uiState.postValue(prepublishingHomeUiStateList)
        _buttonUiState.postValue(
            getButtonUiStateUseCase.getUiState(editPostRepository, site) { publishPost ->
                launch(bgDispatcher) {
                    waitForStoryTitleJobAndSubmit(publishPost)
                }
            }
        )
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showNotSetPost(
        editPostRepository: EditPostRepository,
        site: SiteModel
    ) {
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Tags,
                actionResult = getPostTagsUseCase.getTags(editPostRepository)
                    ?.let { UiStringText(it) }
                    ?: run { UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
                actionClickable = true,
                onNavigationActionClicked = ::onActionClicked
            )
        )

        val categoryString: String = getCategoriesUseCase.getPostCategoriesString(
            editPostRepository,
            site
        )
        if (categoryString.isNotEmpty()) {
            UiStringText(categoryString)
        }
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPrivatePost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Publish,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionTypeColor = R.color.prepublishing_action_type_disabled_color,
                actionResultColor = R.color.prepublishing_action_result_disabled_color,
                actionClickable = false,
                onNavigationActionClicked = null
            )
        )
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPublicPost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Publish,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionClickable = true,
                onNavigationActionClicked = ::onActionClicked
            )
        )
    }

    private suspend fun waitForStoryTitleJobAndSubmit(publishPost: PublishPost) {
        updateStoryTitleJob?.join()
        analyticsTrackerWrapper.trackPrepublishingNudges(AnalyticsTracker.Stat.EDITOR_POST_PUBLISH_NOW_TAPPED)
        _onSubmitButtonClicked.postValue(Event(publishPost))
    }

    override fun onCleared() {
        super.onCleared()
        updateStoryTitleJob?.cancel()
    }

    /**
     * Recomputes the featured-image card off the main thread (createCurrentFeaturedImageState does a
     * media-store lookup). Called after any featured-image change and on upload events so the card
     * reflects empty -> uploading -> set without leaving the sheet. The initial state is seeded
     * synchronously by setupHomeUiState, so this only handles updates.
     *
     * onResume, the postChanged observer and onMediaUploaded can fire near-simultaneously. Runs
     * single-flight: cancelling the previous launch keeps the latest state from being overwritten by a
     * slower in-flight one. Skipping the relaunch while one is active would not be safe, since that job
     * may already have read the pre-change state.
     */
    fun refreshFeaturedImage() {
        if (!isStarted) return
        refreshFeaturedImageJob?.cancel()
        refreshFeaturedImageJob = launch(bgDispatcher) {
            val featuredImageUiState = createFeaturedImageUiState()
            // Bail if a newer refresh superseded this one while the synchronous lookup ran, so a
            // slower/stale result never overwrites the latest state.
            ensureActive()
            _featuredImageUiState.postValue(featuredImageUiState)
        }
    }

    private fun createFeaturedImageUiState(): FeaturedImageUiState {
        if (editPostRepository.isPage || !isFeaturedImageEditingSupported) {
            return FeaturedImageUiState.Hidden
        }
        val post = editPostRepository.getPost() ?: return FeaturedImageUiState.Hidden
        return FeaturedImageUiState.Visible(
            featuredImageData = featuredImageHelper.createCurrentFeaturedImageState(site, post),
            onSetOrReplaceClicked = ::onFeaturedImageSetOrReplaceClicked,
            onRemoveClicked = ::onFeaturedImageRemoveClicked,
            onRetryClicked = ::onFeaturedImageRetryClicked
        )
    }

    private fun onFeaturedImageSetOrReplaceClicked() {
        val post = editPostRepository.getPost() ?: return
        // Replacing mid-upload would otherwise leave two uploads marked as featured.
        featuredImageHelper.cancelFeaturedImageUpload(site, post, false)
        featuredImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_SET_CLICKED, post.id)
        _launchFeaturedImagePicker.postValue(Event(post.id))
    }

    private fun onFeaturedImageRemoveClicked() {
        val post = editPostRepository.getPost() ?: return
        featuredImageHelper.cancelFeaturedImageUpload(site, post, false)
        featuredImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_REMOVE_CLICKED, post.id)
        updateFeaturedImageUseCase.updateFeaturedImage(NO_FEATURED_IMAGE_ID, editPostRepository) {
            refreshFeaturedImage()
            _syncFeaturedImageToEditor.postValue(Event(Unit))
        }
    }

    private fun onFeaturedImageRetryClicked() {
        val post = editPostRepository.getPost() ?: return
        featuredImageHelper.retryFeaturedImageUpload(site, post)
        refreshFeaturedImage()
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }

    fun updateJetpackSocialState(
        state: EditorJetpackSocialViewModel.JetpackSocialUiState?
    ) {
        val isPostPublished = editPostRepository.getPost()?.status == PostStatus.PUBLISHED.toString()
        val newState = state?.takeUnless { isPostPublished }?.let {
            SocialUiState.Visible(
                it,
                onItemClicked = { onActionClicked(PrepublishingScreenNavigation.Social) }
            )
        } ?: SocialUiState.Hidden

        _socialUiState.postValue(newState)
    }

    companion object {
        private const val NO_FEATURED_IMAGE_ID = 0L
    }
}
