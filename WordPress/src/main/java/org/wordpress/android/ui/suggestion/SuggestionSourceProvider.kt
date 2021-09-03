package org.wordpress.android.ui.suggestion

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import javax.inject.Inject

class SuggestionSourceProvider @Inject constructor(
    private val suggestionSourceSubcomponentFactory: SuggestionSourceSubcomponent.Factory
) {
    fun get(type: SuggestionType, site: SiteModel): SuggestionSource {
        val factory = suggestionSourceSubcomponentFactory.create(site)
        return when (type) {
            XPosts -> factory.xPostSuggestionSource()
            Users -> factory.userSuggestionSource()
        }
    }
}
