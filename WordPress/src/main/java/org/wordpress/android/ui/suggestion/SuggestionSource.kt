package org.wordpress.android.ui.suggestion

import androidx.lifecycle.LiveData
import org.wordpress.android.fluxc.model.SiteModel

interface SuggestionSource {
    val site: SiteModel
    val suggestionData: LiveData<SuggestionResult>
    fun refreshSuggestions()
    fun onCleared()
    fun isFetchInProgress(): Boolean
}

data class SuggestionResult(val suggestions: List<Suggestion>, val hadFetchError: Boolean)
