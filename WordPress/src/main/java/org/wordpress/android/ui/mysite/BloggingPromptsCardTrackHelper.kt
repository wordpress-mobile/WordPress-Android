package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
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

    private fun onDashboardRefreshed(state: MySiteViewModel.State.SiteSelected?) {
        val bloggingPromptCards = state?.dashboardData
            ?.filterIsInstance<BloggingPromptCard>()
            ?: listOf()

        latestPromptCardVisible.get()?.let { isPromptCardVisible ->
            val attribution = bloggingPromptCards.firstOrNull()?.attribution
            if (isPromptCardVisible) tracker.trackMySiteCardViewed(attribution)
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }


    fun onDashboardCardsUpdated(scope: CoroutineScope, state: MySiteViewModel.State.SiteSelected?) {
        val bloggingPromptCards = state?.dashboardData
            ?.filterIsInstance<BloggingPromptCard>()
            ?: listOf()

        // cancel any existing job (debouncing mechanism)
        dashboardUpdateDebounceJob?.cancel()

        dashboardUpdateDebounceJob = scope.launch(bgDispatcher) {
            val isVisible = bloggingPromptCards.isNotEmpty()

            // add a delay (debouncing mechanism)
            delay(PROMPT_CARD_VISIBLE_DEBOUNCE)

            latestPromptCardVisible.set(isVisible)
            if (isVisible && waitingToTrack.getAndSet(false)) {
                val attribution = bloggingPromptCards.firstOrNull()?.attribution
                tracker.trackMySiteCardViewed(attribution)
            }
        }.also {
            it.invokeOnCompletion { cause ->
                // only set the job to null if it wasn't cancelled since cancellation is part of debouncing
                if (cause == null) dashboardUpdateDebounceJob = null
            }
        }
    }

    fun onResume(state: MySiteViewModel.State.SiteSelected?) {
        onDashboardRefreshed(state)
    }

    fun onSiteChanged(siteId: Int?, state: MySiteViewModel.State.SiteSelected?) {
        if (currentSite.getAndSet(siteId) != siteId) {
            latestPromptCardVisible.set(null)
            onDashboardRefreshed(state)
        }
    }

    private val BloggingPromptCard.attribution: String?
        get() {
            return (this as? BloggingPromptCard.BloggingPromptCardWithData)?.attribution?.value
        }

    companion object {
        private const val PROMPT_CARD_VISIBLE_DEBOUNCE = 500L
    }
}
