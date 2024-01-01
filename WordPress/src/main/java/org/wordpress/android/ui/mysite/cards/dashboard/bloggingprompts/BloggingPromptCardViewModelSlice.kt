package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsPostTagProvider
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.BloggingPromptCardNavigationAction
import org.wordpress.android.ui.mysite.BloggingPromptsCardTrackHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.BloggingPromptsFeature
import org.wordpress.android.viewmodel.Event
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val NUM_PROMPTS_TO_REQUEST = 20

class BloggingPromptCardViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val bloggingPromptsCardTrackHelper: BloggingPromptsCardTrackHelper,
    private val bloggingPromptsPostTagProvider: BloggingPromptsPostTagProvider,
    private val bloggingPromptCardBuilder: BloggingPromptCardBuilder,
    private val promptsStore: BloggingPromptsStore,
    private val bloggingPromptsFeature: BloggingPromptsFeature
) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage as LiveData<Event<SnackbarMessageHolder>>

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<BloggingPromptCardWithData?>()
    val uiModel = _uiModel.distinctUntilChanged()

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh: LiveData<Event<Boolean>> = _refresh

    private lateinit var scope: CoroutineScope

    fun buildCard(
        siteModel: SiteModel
    ) {
        if (bloggingPromptsFeature.isEnabled()) {
            scope.launch(bgDispatcher) {
                if (bloggingPromptsSettingsHelper.shouldShowPromptsFeature()) {
                    promptsStore.getPrompts(siteModel)
                        .map { it.model?.filter { prompt -> isSameDay(prompt.date, Date()) } }
                        .collect { result ->
                            postState(result?.firstOrNull())
                        }
                } else {
                    postEmptyState()
                }
            }
        } else {
            postLastState()
        }
    }

    fun refreshData(
        siteModel: SiteModel,
        isSinglePromptRefresh: Boolean = false
    ) {
        if (bloggingPromptsFeature.isEnabled()) {
            scope.launch(bgDispatcher) {
                if (bloggingPromptsSettingsHelper.shouldShowPromptsFeature()) {
                    fetchPromptsAndPostErrorIfAvailable(siteModel, isSinglePromptRefresh)
                } else {
                    postEmptyState()
                }
            }
        } else {
            postEmptyState()
        }
    }

    private fun fetchPromptsAndPostErrorIfAvailable(
        selectedSite: SiteModel,
        isSinglePromptRefresh: Boolean = false
    ) {
        scope.launch(bgDispatcher) {
            val numOfPromptsToFetch = if (isSinglePromptRefresh) 1 else NUM_PROMPTS_TO_REQUEST
            val result = promptsStore.fetchPrompts(selectedSite, numOfPromptsToFetch, Date())
            when {
                result.isError -> postLastState()
                else -> {
                    result.model
                        ?.firstOrNull { prompt -> isSameDay(prompt.date, Date()) }
                        ?.let { prompt -> postState(prompt) }
                        ?: postLastState()
                }
            }
        }
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun buildCard(bloggingPromptUpdate: BloggingPromptModel): BloggingPromptCardWithData? {
        return bloggingPromptCardBuilder.build(getBuilderParams(bloggingPromptUpdate))
    }

    fun getBuilderParams(bloggingPromptModel: BloggingPromptModel):
            MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams {
        return MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams(
            bloggingPrompt = bloggingPromptModel,
            onShareClick = this::onBloggingPromptShareClick,
            onAnswerClick = { id -> onBloggingPromptAnswerClick(id, bloggingPromptModel.attribution) },
            onSkipClick = this::onBloggingPromptSkipClick,
            onViewMoreClick = this::onBloggingPromptViewMoreClick,
            onViewAnswersClick = this::onBloggingPromptViewAnswersClick,
            onRemoveClick = this::onBloggingPromptRemoveClick
        )
    }

    private fun onBloggingPromptShareClick(message: String) {
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.SharePrompt(message))
    }

    private fun onBloggingPromptAnswerClick(promptId: Int, attribution: String?) {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardAnswerPromptClicked(attribution)
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.AnswerPrompt(selectedSite, promptId))
    }

    private fun onBloggingPromptSkipClick() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            val siteId = site.localId().value

            appPrefsWrapper.setSkippedPromptDay(Date(), siteId)
            _refresh.postValue(Event(true))

            val snackbar = SnackbarMessageHolder(
                message = UiString.UiStringRes(R.string.my_site_blogging_prompt_card_skipped_snackbar),
                buttonTitle = UiString.UiStringRes(R.string.undo),
                buttonAction = {
                    bloggingPromptsCardAnalyticsTracker.trackMySiteCardSkipThisPromptUndoClicked()
                    appPrefsWrapper.setSkippedPromptDay(null, siteId)
                    _refresh.postValue(Event(true))
                },
                isImportant = true
            )

            _onSnackbarMessage.value = Event(snackbar)
        }
    }

    private fun onBloggingPromptViewAnswersClick(tagUrl: String) {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardViewAnswersClicked()
        val tag = bloggingPromptsPostTagProvider.promptIdSearchReaderTag(tagUrl)
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.ViewAnswers(tag))
    }

    private fun onBloggingPromptViewMoreClick() {
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.ViewMore)
    }

    private fun onBloggingPromptRemoveClick() {
        scope.launch(bgDispatcher) {
            updatePromptsCardEnabled(isEnabled = false).join()
            _onNavigation.postValue(
                Event(
                    BloggingPromptCardNavigationAction
                        .CardRemoved(this@BloggingPromptCardViewModelSlice::onBloggingPromptUndoClick)
                )
            )
        }
    }


    /**
     * This function is used to make sure the [refresh] information is propagated and processed correctly even though
     * the previous status is still the current one. This avoids issues like the loading progress indicator being shown
     * indefinitely.
     *
     * Also, for this card source, this can be used as the error state as we don't have any special error handling at
     * this point, so we just show the last available prompt.
     */
    private fun postLastState() {
        _isRefreshing.postValue(false)
        //do nothing WIP @ajesh: remove this once this case doesnt occur on the new architecture
    }

    private fun postEmptyState() {
        _isRefreshing.postValue(false)
        postState(null)
    }

    private fun postState(bloggingPrompt: BloggingPromptModel?) {
        _isRefreshing.postValue(false)
        bloggingPrompt?.let {
            buildCard(bloggingPrompt)?.let { card ->
                _uiModel.postValue(card)
            }
        }?: _uiModel.postValue(null)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val localDate1: LocalDate = date1.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val localDate2: LocalDate = date2.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate1.isEqual(localDate2)
    }

    private fun onBloggingPromptUndoClick() {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardRemoveFromDashboardUndoClicked()
        updatePromptsCardEnabled(true)
    }

    private fun updatePromptsCardEnabled(isEnabled: Boolean) = scope.launch(bgDispatcher) {
        selectedSiteRepository.getSelectedSite()?.localId()?.value?.let { siteId ->
            bloggingPromptsSettingsHelper.updatePromptsCardEnabled(siteId, isEnabled)
            _refresh.postValue(Event(true))
        }
    }

    fun onDashboardCardsUpdated(
        scope: CoroutineScope,
        state: MySiteViewModel.State.SiteSelected?
    ) {
        bloggingPromptsCardTrackHelper.onDashboardCardsUpdated(scope, state)
    }

    fun onSiteChanged(siteId: Int?, state: MySiteViewModel.State.SiteSelected?) {
        bloggingPromptsCardTrackHelper.onSiteChanged(siteId, state)
    }

    fun onResume(state: MySiteViewModel.State.SiteSelected?) {
        bloggingPromptsCardTrackHelper.onResume(state)
    }
}
