package org.wordpress.android.ui.suggestion.util

import android.content.Context
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.UserSuggestion
import org.wordpress.android.ui.suggestion.Suggestion
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.util.SiteUtils

object SuggestionUtils {
    @JvmStatic
    fun setupUserSuggestions(
        site: SiteModel,
        context: Context,
        connectionManager: SuggestionServiceConnectionManager
    ): SuggestionAdapter = setupUserSuggestions(
            site.siteId,
            context,
            connectionManager,
            SiteUtils.isAccessedViaWPComRest(site)
    )

    @JvmStatic
    fun setupUserSuggestions(
        siteId: Long,
        context: Context,
        connectionManager: SuggestionServiceConnectionManager,
        isWPCom: Boolean
    ): SuggestionAdapter {
        val initialSuggestions = setupUserSuggestions(siteId, connectionManager, isWPCom)
        return SuggestionAdapter(context, '@').apply {
            suggestionList = Suggestion.fromUserSuggestions(initialSuggestions)
        }
    }

    private fun setupUserSuggestions(
        siteId: Long,
        serviceConnectionManager: SuggestionServiceConnectionManager,
        isWPCom: Boolean
    ): List<UserSuggestion> {
        if (!isWPCom) {
            return emptyList()
        }
        serviceConnectionManager.bindToService()

        // Immediately return any already saved suggestions
        return UserSuggestionTable.getSuggestionsForSite(siteId) ?: emptyList()
    }
}
