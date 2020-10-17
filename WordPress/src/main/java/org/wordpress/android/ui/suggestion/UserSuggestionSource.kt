package org.wordpress.android.ui.suggestion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import javax.inject.Inject

class UserSuggestionSource @Inject constructor(
    context: Context,
    override val site: SiteModel
) : SuggestionSource {
    private val connectionManager = SuggestionServiceConnectionManager(context, site.siteId)

    private val _suggestions = MutableLiveData<List<Suggestion>>()
    override val suggestions: LiveData<List<Suggestion>> = _suggestions

    init {
        _suggestions.postValue(savedSuggestions())
        connectionManager.bindToService()
    }

    override fun onSuggestionsUpdated() {
        _suggestions.postValue(savedSuggestions())
    }

    private fun savedSuggestions() =
            Suggestion.fromUserSuggestions(
                    UserSuggestionTable.getSuggestionsForSite(site.siteId)
            )

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
