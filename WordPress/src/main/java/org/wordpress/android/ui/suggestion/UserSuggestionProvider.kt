package org.wordpress.android.ui.suggestion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.datasets.SuggestionTable
import org.wordpress.android.models.Suggestion
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager

class UserSuggestionProvider(context: Context, override val siteId: Long) : SuggestionProvider {
    private val connectionManager = SuggestionServiceConnectionManager(context, siteId).apply {
        bindToService()
    }

    private val _suggestions = MutableLiveData<List<Suggestion>>().apply {
        value = savedSuggestions()
    }
    override val suggestions: LiveData<List<Suggestion>> = _suggestions

    override fun onSuggestionsUpdated() {
        _suggestions.value = savedSuggestions()
    }

    private fun savedSuggestions() = SuggestionTable.getSuggestionsForSite(siteId)

    override fun refreshSuggestions() {
        connectionManager.apply {
            unbindFromService()
            bindToService()
        }
    }

    override fun onDestroy() {
        connectionManager.unbindFromService()
    }
}
