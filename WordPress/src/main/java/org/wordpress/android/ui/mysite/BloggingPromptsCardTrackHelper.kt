package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Helper class to coordinate some My Site Dashboard events and trigger the appropriate Blogging Prompts Card viewed
 * event. This basically observes when the [MySiteViewModel] resumes, when the site ID changes, and whether the Prompts
 * card is visible, coordinating to figure out the current state and then send the
 * [BloggingPromptsCardAnalyticsTracker.trackMySiteCardViewed] event if identified that the user, on the current site,
 * has the Dashboard selected and the Prompts card is showing in that Dashboard.
 */
class BloggingPromptsCardTrackHelper(
    private val scope: CoroutineScope,
    private val tracker: BloggingPromptsCardAnalyticsTracker,
    private val siteIdFlow: Flow<Int?>,
    dashboardCardsFlow: Flow<List<DashboardCards>>,
) {
    private val latestPromptCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)

    private val onDashboardRefreshed = MutableSharedFlow<Unit>()
    private val trackBloggingPromptCardShownFlow = MutableSharedFlow<Unit>()

    @OptIn(FlowPreview::class)
    private val onBloggingPromptsCardVisible: Flow<Boolean> = dashboardCardsFlow
        .map {
            it.firstOrNull()
                ?.cards
                ?.any { card -> card is BloggingPromptCard }
                ?: false
        }
        .debounce(PROMPT_CARD_VISIBLE_DEBOUNCE) // this flows emits several times so add some debounce to it

    init {
        trackBloggingPromptCardShownFlow
            .onEach { tracker.trackMySiteCardViewed() }
            .launchIn(scope)

        scope.launch {
            val currentSite = AtomicReference<Int?>(null)

            siteIdFlow
                .distinctUntilChanged()
                .onEach { siteId ->
                    if (currentSite.getAndSet(siteId) != siteId) {
                        latestPromptCardVisible.set(null)
                        onDashboardRefreshed.emit(Unit)
                    }
                }
                .launchIn(this)
        }

        scope.launch {
            val outerScope = this

            launch {
                try {
                    onBloggingPromptsCardVisible.collect { isVisible ->
                        latestPromptCardVisible.set(isVisible)
                        if (isVisible && waitingToTrack.getAndSet(false)) {
                            trackBloggingPromptCardShownFlow.emit(Unit)
                        }
                    }
                } catch (e: CancellationException) {
                    outerScope.cancel(e)
                }
            }

            onDashboardRefreshed.collect {
                latestPromptCardVisible.get()?.let { isPromptCardVisible ->
                    if (isPromptCardVisible) trackBloggingPromptCardShownFlow.emit(Unit)
                    waitingToTrack.set(false)
                } ?: run {
                    waitingToTrack.set(true)
                }
            }
        }
    }

    fun onResume(currentTab: MySiteTabType) {
        if (currentTab == MySiteTabType.DASHBOARD) {
            scope.launch { onDashboardRefreshed.emit(Unit) }
        } else {
            // moved away from dashboard, no longer waiting to track
            waitingToTrack.set(false)
        }
    }

    companion object {
        private const val PROMPT_CARD_VISIBLE_DEBOUNCE = 500L
    }
}
