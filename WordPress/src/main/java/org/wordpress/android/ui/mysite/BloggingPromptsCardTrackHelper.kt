package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var dashboardUpdateDebounceJob: Job? = null

    private val latestPromptCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    private fun onDashboardRefreshed() {
        latestPromptCardVisible.get()?.let { isPromptCardVisible ->
            if (isPromptCardVisible) tracker.trackMySiteCardViewed()
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }


    fun onDashboardCardsUpdated(scope: CoroutineScope, dashboard: DashboardCards?) {
        // cancel any existing job (debouncing mechanism)
        dashboardUpdateDebounceJob?.cancel()

        dashboardUpdateDebounceJob = scope.launch(bgDispatcher) {
            val isVisible = dashboard?.cards?.any { card -> card is BloggingPromptCard } ?: false

            // add a delay (debouncing mechanism)
            delay(PROMPT_CARD_VISIBLE_DEBOUNCE)

            latestPromptCardVisible.set(isVisible)
            if (isVisible && waitingToTrack.getAndSet(false)) {
                tracker.trackMySiteCardViewed()
            }
        }.also {
            it.invokeOnCompletion { cause ->
                // only set the job to null if it wasn't cancelled since cancellation is part of debouncing
                if (cause == null) dashboardUpdateDebounceJob = null
            }
        }
    }

    fun onResume(currentTab: MySiteTabType) {
        if (currentTab == MySiteTabType.DASHBOARD) {
            onDashboardRefreshed()
        } else {
            // moved away from dashboard, no longer waiting to track
            waitingToTrack.set(false)
        }
    }

    fun onSiteChanged(siteId: Int?) {
        if (currentSite.getAndSet(siteId) != siteId) {
            latestPromptCardVisible.set(null)
            onDashboardRefreshed()
        }
    }

    companion object {
        private const val PROMPT_CARD_VISIBLE_DEBOUNCE = 500L
    }
}
