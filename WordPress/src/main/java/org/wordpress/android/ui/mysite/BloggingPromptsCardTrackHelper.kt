package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named

/**
 * Helper class to coordinate some My Site Dashboard events and trigger the appropriate Blogging Prompts Card viewed
 * event. This basically observes when the [MySiteViewModel] resumes, when the site ID changes, and whether the Prompts
 * card is visible, coordinating to figure out the current state and then send the
 * [BloggingPromptsCardAnalyticsTracker.trackMySiteCardViewed] event if identified that the user, on the current site,
 * has the Dashboard selected and the Prompts card is showing in that Dashboard.
 */
class BloggingPromptsCardTrackHelper @Inject constructor(
    private val tracker: BloggingPromptsCardAnalyticsTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var scope: CoroutineScope? = null

    private val latestPromptCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    private val onDashboardRefreshed = MutableSharedFlow<Unit>()
    private val promptsCardVisibilityChanged = MutableSharedFlow<Boolean>()

    @OptIn(FlowPreview::class)
    private val onBloggingPromptsCardVisible: Flow<Boolean> = promptsCardVisibilityChanged
        .debounce(PROMPT_CARD_VISIBLE_DEBOUNCE)

    fun initialize(parentScope: CoroutineScope) {
        val newScope = CoroutineScope(parentScope.coroutineContext + bgDispatcher)
        scope = newScope

        // experiment
        onBloggingPromptsCardVisible
            .onEach { isVisible ->
                latestPromptCardVisible.set(isVisible)
                if (isVisible && waitingToTrack.getAndSet(false)) {
                    tracker.trackMySiteCardViewed()
                }
            }
            .launchIn(newScope)

        onDashboardRefreshed
            .onEach {
                latestPromptCardVisible.get()?.let { isPromptCardVisible ->
                    if (isPromptCardVisible) tracker.trackMySiteCardViewed()
                    waitingToTrack.set(false)
                } ?: run {
                    waitingToTrack.set(true)
                }
            }
            .launchIn(newScope)
    }

    fun onResume(currentTab: MySiteTabType) {
        if (currentTab == MySiteTabType.DASHBOARD) {
            scope?.launch { onDashboardRefreshed.emit(Unit) }
        } else {
            // moved away from dashboard, no longer waiting to track
            waitingToTrack.set(false)
        }
    }

    fun onDashboardCardsUpdated(dashboard: DashboardCards?) {
        scope?.launch {
            val isPromptsCardVisible = dashboard?.cards?.any { card -> card is BloggingPromptCard } ?: false
            promptsCardVisibilityChanged.emit(isPromptsCardVisible)
        }
    }

    fun onSiteChanged(siteId: Int?) {
        scope?.launch {
            if (currentSite.getAndSet(siteId) != siteId) {
                latestPromptCardVisible.set(null)
                onDashboardRefreshed.emit(Unit)
            }
        }
    }

    companion object {
        private const val PROMPT_CARD_VISIBLE_DEBOUNCE = 500L
    }
}
