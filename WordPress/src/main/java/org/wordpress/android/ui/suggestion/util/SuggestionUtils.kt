package org.wordpress.android.ui.suggestion.util

import android.content.Context
import org.wordpress.android.datasets.SuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.Suggestion
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.util.SiteUtils

object SuggestionUtils {
    @JvmStatic
    fun setupSuggestions(
        site: SiteModel,
        context: Context,
        connectionManager: SuggestionServiceConnectionManager
    ): SuggestionAdapter = setupSuggestions(
            site.siteId,
            context,
            connectionManager,
            SiteUtils.isAccessedViaWPComRest(site)
    )

    @JvmStatic
    fun setupSuggestions(
        siteId: Long,
        context: Context,
        connectionManager: SuggestionServiceConnectionManager,
        isWPCom: Boolean
    ): SuggestionAdapter {
        val initialSuggestions = setupSuggestions(siteId, connectionManager, isWPCom)
        return SuggestionAdapter(context).apply {
            suggestionList = initialSuggestions
        }
    }

    @JvmStatic
    fun setupSuggestions(
        siteId: Long,
        serviceConnectionManager: SuggestionServiceConnectionManager,
        isWPCom: Boolean
    ): List<Suggestion> {
        if (!isWPCom) {
            return emptyList()
        }
        serviceConnectionManager.bindToService()

        // Immediately return any already saved suggestions
        return SuggestionTable.getSuggestionsForSite(siteId) ?: emptyList()
    }
}
