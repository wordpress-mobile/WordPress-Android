package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsPostTagProvider
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.BloggingPromptCardNavigationAction
import org.wordpress.android.ui.mysite.BloggingPromptsCardTrackHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteSourceManager
import org.wordpress.android.ui.mysite.MySiteUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BloggingPromptCardViewModelSlice @Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val mySiteSourceManager: MySiteSourceManager,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val bloggingPromptsCardTrackHelper: BloggingPromptsCardTrackHelper
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage as LiveData<Event<SnackbarMessageHolder>>

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun getBuilderParams(bloggingPromptUpdate: MySiteUiState.PartialState.BloggingPromptUpdate?):
            MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams {
        return MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams(
            bloggingPrompt = bloggingPromptUpdate?.promptModel,
            onShareClick = this::onBloggingPromptShareClick,
            onAnswerClick = this::onBloggingPromptAnswerClick,
            onSkipClick = this::onBloggingPromptSkipClick,
            onViewMoreClick = this::onBloggingPromptViewMoreClick,
            onViewAnswersClick = this::onBloggingPromptViewAnswersClick,
            onRemoveClick = this::onBloggingPromptRemoveClick
        )
    }

    private fun onBloggingPromptShareClick(message: String) {
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.SharePrompt(message))
    }

    private fun onBloggingPromptAnswerClick(promptId: Int) {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardAnswerPromptClicked()
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.AnswerPrompt(selectedSite, promptId))
    }

    private fun onBloggingPromptSkipClick() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            val siteId = site.localId().value

            appPrefsWrapper.setSkippedPromptDay(Date(), siteId)
            mySiteSourceManager.refreshBloggingPrompts(true)

            val snackbar = SnackbarMessageHolder(
                message = UiString.UiStringRes(R.string.my_site_blogging_prompt_card_skipped_snackbar),
                buttonTitle = UiString.UiStringRes(R.string.undo),
                buttonAction = {
                    bloggingPromptsCardAnalyticsTracker.trackMySiteCardSkipThisPromptUndoClicked()
                    appPrefsWrapper.setSkippedPromptDay(null, siteId)
                    mySiteSourceManager.refreshBloggingPrompts(true)
                },
                isImportant = true
            )

            _onSnackbarMessage.value = Event(snackbar)
        }
    }

    private fun onBloggingPromptViewAnswersClick(promptId: Int) {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardViewAnswersClicked()
        val tag = BloggingPromptsPostTagProvider.promptIdSearchReaderTag(promptId)
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.ViewAnswers(tag))
    }

    private fun onBloggingPromptViewMoreClick() {
        _onNavigation.value = Event(BloggingPromptCardNavigationAction.ViewMore)
    }

    private fun onBloggingPromptRemoveClick() {
        launch {
            updatePromptsCardEnabled(isEnabled = false).join()
            _onNavigation.value = Event(BloggingPromptCardNavigationAction
                .CardRemoved(this@BloggingPromptCardViewModelSlice::onBloggingPromptUndoClick))
        }
    }

    fun onBloggingPromptUndoClick() {
        bloggingPromptsCardAnalyticsTracker.trackMySiteCardRemoveFromDashboardUndoClicked()
        updatePromptsCardEnabled(true)
    }

    private fun updatePromptsCardEnabled(isEnabled: Boolean) = launch {
        selectedSiteRepository.getSelectedSite()?.localId()?.value?.let { siteId ->
            bloggingPromptsSettingsHelper.updatePromptsCardEnabled(siteId, isEnabled)
            mySiteSourceManager.refreshBloggingPrompts(true)
        }
    }

    fun onDashboardCardsUpdated(scope: CoroutineScope, dashboard: MySiteCardAndItem.Card.DashboardCards?) {
        bloggingPromptsCardTrackHelper.onDashboardCardsUpdated(scope, dashboard)
    }

    fun onSiteChanged(siteId: Int?) {
        bloggingPromptsCardTrackHelper.onSiteChanged(siteId)
    }

    fun onResume(currentTab: MySiteTabType) {
        bloggingPromptsCardTrackHelper.onResume(currentTab)
    }
}
