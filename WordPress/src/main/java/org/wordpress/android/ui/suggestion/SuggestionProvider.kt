package org.wordpress.android.ui.suggestion

import androidx.lifecycle.LiveData
import org.wordpress.android.models.Suggestion

interface SuggestionProvider {
    val siteId: Long
    val suggestions: LiveData<List<Suggestion>>
    fun onSuggestionsUpdated()
    fun refreshSuggestions()
    fun onDestroy()
}
