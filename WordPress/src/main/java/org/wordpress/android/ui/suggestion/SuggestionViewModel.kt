package org.wordpress.android.ui.suggestion

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SuggestionViewModel @Inject constructor(
    private val suggestionSourceProvider: SuggestionSourceProvider,
    private val resourceProvider: ResourceProvider,
    private val networkUtils: NetworkUtilsWrapper
) : ViewModel() {
    private lateinit var suggestionSource: SuggestionSource
    private lateinit var type: SuggestionType
    val suggestions: LiveData<List<Suggestion>>
        get() = suggestionSource.suggestions

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
        val hasSuggestions = suggestions.value?.isNotEmpty() == true

        val text = when {
            hasSuggestions -> resourceProvider.getString(
                    R.string.suggestion_no_matching,
                    suggestionTypeString
            )
            networkUtils.isNetworkAvailable() -> resourceProvider.getString(R.string.loading)
            else -> resourceProvider.getString(R.string.suggestion_no_connection)
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
            val message = resourceProvider.getString(
                    R.string.suggestion_invalid,
                    currentUserInput,
                    suggestionTypeString
            )
            NotExactlyOneAvailable(message)
        }
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
