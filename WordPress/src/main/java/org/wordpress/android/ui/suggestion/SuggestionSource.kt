package org.wordpress.android.ui.suggestion

import androidx.lifecycle.LiveData
import org.wordpress.android.fluxc.model.SiteModel

interface SuggestionSource {
    val site: SiteModel
    val suggestions: LiveData<List<Suggestion>>
    fun onSuggestionsUpdated()
    fun refreshSuggestions()
    fun onDestroy()
}
