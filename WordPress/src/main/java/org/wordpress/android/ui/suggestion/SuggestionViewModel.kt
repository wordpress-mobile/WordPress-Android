package org.wordpress.android.ui.suggestion

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SuggestionViewModel @Inject constructor(
    private val suggestionSourceProvider: SuggestionSourceProvider,
    private val resourceProvider: ResourceProvider,
    private val networkUtils: NetworkUtilsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    private lateinit var suggestionSource: SuggestionSource
    private lateinit var type: SuggestionType
    val suggestionData: LiveData<SuggestionResult>
        get() = suggestionSource.suggestionData

    val suggestionPrefix: Char by lazy {
        when (type) {
            XPosts -> '+'
            Users -> '@'
        }
    }

    val suggestionTypeString: String by lazy {
        resourceProvider.getString(
            when (type) {
                XPosts -> R.string.suggestion_xpost
                Users -> R.string.suggestion_user
            }
        )
    }

    fun init(type: SuggestionType, site: SiteModel) =
        if (supportsSuggestions(site)) {
            this.type = type
            suggestionSource = suggestionSourceProvider.get(type, site)
            suggestionSource.initialize()
            true
        } else {
            AppLog.e(T.EDITOR, "Attempting to initialize suggestions for an unsupported site")
            false
        }

    private fun supportsSuggestions(site: SiteModel): Boolean = SiteUtils.isAccessedViaWPComRest(site)

    fun onConnectionChanged(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            suggestionSource.refreshSuggestions()
        }
    }

    fun getEmptyViewState(displayedSuggestions: List<Suggestion>?): EmptyViewState {
        val hasSuggestions = suggestionData.value?.suggestions?.isNotEmpty() == true

        val text = when {
            hasSuggestions -> {
                // Displaying the empty view even though we have suggestions means that the user is filtering out
                // all the suggestions.
                resourceProvider.getString(R.string.suggestion_no_matching, suggestionTypeString)
            }
            networkUtils.isNetworkAvailable() -> when {
                suggestionSource.isFetchInProgress() -> {
                    resourceProvider.getString(R.string.loading)
                }
                suggestionData.value?.hadFetchError == true -> {
                    resourceProvider.getString(R.string.suggestion_problem)
                }
                else -> {
                    // We have a suggestion update that we know was empty (because we already checked
                    // `hasSuggestions` in the parent when statement), and there wasn't a fetch error,
                    // so notify the user that there are no suggestions available
                    resourceProvider.getString(R.string.suggestion_none, suggestionTypeString)
                }
            }
            else -> {
                // Only provide this error message if we cannot provide any better information
                resourceProvider.getString(R.string.suggestion_no_connection)
            }
        }

        val visibility = if (displayedSuggestions?.isNotEmpty() == true) View.GONE else View.VISIBLE

        return EmptyViewState(text, visibility)
    }

    fun onAttemptToFinish(
        selections: List<Suggestion>?,
        currentUserInput: String
    ): FinishAttempt {
        val onlyDisplayedSuggestion = if (selections?.size == 1) {
            selections.first()
        } else {
            null
        }

        return if (onlyDisplayedSuggestion != null) {
            OnlyOneAvailable(onlyDisplayedSuggestion.value)
        } else {
            // Provide different message depending on whether the user has tried to "submit" a suggestion
            // based on no filter text versus tried to submit a suggestion based on text that just doesn't
            // match a single suggestion.
            val message = if (currentUserInput == suggestionPrefix.toString()) {
                resourceProvider.getString(R.string.suggestion_selection_needed)
            } else {
                resourceProvider.getString(
                    R.string.suggestion_invalid,
                    currentUserInput,
                    suggestionTypeString
                )
            }
            NotExactlyOneAvailable(message)
        }
    }

    fun trackExit(withSuggestion: Boolean) {
        val trackingSuggestionType = when (type) {
            XPosts -> "xpost"
            Users -> "user"
        }
        val properties = mapOf(
            "did_select_suggestion" to withSuggestion,
            "suggestion_type" to trackingSuggestionType
        )
        analyticsTracker.track(AnalyticsTracker.Stat.SUGGESTION_SESSION_FINISHED, properties)
    }

    override fun onCleared() {
        suggestionSource.onCleared()
        super.onCleared()
    }
}

data class EmptyViewState(val string: String, val visibility: Int)

sealed class FinishAttempt {
    data class OnlyOneAvailable(val onlySelectedValue: String) : FinishAttempt()
    data class NotExactlyOneAvailable(val errorMessage: String) : FinishAttempt()
}
