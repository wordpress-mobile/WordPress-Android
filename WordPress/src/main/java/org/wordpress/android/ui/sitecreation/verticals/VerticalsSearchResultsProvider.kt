package org.wordpress.android.ui.sitecreation.verticals

import dagger.Reusable
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

@Reusable
class VerticalsSearchResultsProvider @Inject constructor(localeManagerWrapper: LocaleManagerWrapper) {
    private val locale = localeManagerWrapper.getLocale()

    fun search(items: List<IntentListItemUiState>, query: String) = if (query.isEmpty()) items else items.filter {
        val text = it.verticalText.lowercase(locale)
        val lowercasedQuery = query.lowercase(locale)
        text.contains(lowercasedQuery)
    }
}
