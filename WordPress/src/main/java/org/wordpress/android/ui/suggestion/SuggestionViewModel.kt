package org.wordpress.android.ui.suggestion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.Suggestion
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject

class SuggestionViewModel @Inject constructor(): ViewModel() {
    private lateinit var suggestionProvider: SuggestionProvider

    fun init(context: Context, site: SiteModel): LiveData<List<Suggestion>>? {
        if (!SiteUtils.isAccessedViaWPComRest(site)) {
            return null
        }
        suggestionProvider = UserSuggestionProvider(context, site.siteId)
        return suggestionProvider.suggestions
    }

    fun onSuggestionsUpdated(updatedSiteId: Long) {
        if (updatedSiteId == suggestionProvider.siteId) {
            suggestionProvider.onSuggestionsUpdated()
        }
    }

    fun onConnectionChanged(event: ConnectionChangeEvent) {
        val hasNoSuggestions = suggestionProvider.suggestions.value?.isEmpty() == true
        if (event.isConnected && hasNoSuggestions) {
            suggestionProvider.refreshSuggestions()
        }
    }

    fun onDestroy() {
        suggestionProvider.onDestroy()
    }
}
