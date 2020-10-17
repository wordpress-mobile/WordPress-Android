package org.wordpress.android.ui.suggestion

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject

class SuggestionViewModel @Inject constructor(
    private val suggestionSourceSubcomponentFactory: SuggestionSourceSubcomponent.Factory
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

    val suggestionTypeStringRes: Int by lazy {
        when (type) {
            XPosts -> R.string.suggestion_xpost
            Users -> R.string.suggestion_user
        }
    }

    fun init(type: SuggestionType, site: SiteModel) =
            if (supportsSuggestions(site)) {
                this.type = type
                suggestionSource = getSuggestionSourceForType(type, site)
                true
            } else {
                AppLog.e(T.EDITOR, "Attempting to initialize suggestions for an unsupported site")
                false
            }

    private fun supportsSuggestions(site: SiteModel): Boolean = SiteUtils.isAccessedViaWPComRest(site)

    private fun getSuggestionSourceForType(
        type: SuggestionType,
        site: SiteModel
    ): SuggestionSource {
        val suggestionSourceSubcomponent = suggestionSourceSubcomponentFactory.create(site)
        return when (type) {
            Users -> suggestionSourceSubcomponent.userSuggestionSource()
            XPosts -> suggestionSourceSubcomponent.xPostSuggestionSource()
        }
    }

    fun onSuggestionsUpdated(updatedSiteId: Long) {
        if (updatedSiteId == suggestionSource.site.siteId) {
            suggestionSource.onSuggestionsUpdated()
        }
    }

    fun onConnectionChanged(event: ConnectionChangeEvent) {
        val hasNoSuggestions = suggestionSource.suggestions.value?.isEmpty() == true
        if (event.isConnected && hasNoSuggestions) {
            suggestionSource.refreshSuggestions()
        }
    }

    fun onDestroy() {
        suggestionSource.onDestroy()
    }
}
