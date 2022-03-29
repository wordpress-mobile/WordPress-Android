package org.wordpress.android.ui.sitecreation.verticals

import dagger.Reusable
import org.apache.commons.text.similarity.FuzzyScore
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

@Reusable
class VerticalsSearchResultsProvider @Inject constructor(localeManagerWrapper: LocaleManagerWrapper) {
    private val locale = localeManagerWrapper.getLocale()
    private val fuzzyScore = FuzzyScore(locale)

    fun search(items: List<IntentListItemUiState>, query: String) = if (query.isEmpty()) items else items.filter {
        val text = it.verticalText.lowercase(locale)
        val query = query.lowercase(locale)
        fuzzyScore.fuzzyScore(query, text) > fuzzyScoreThreshold
    }

    companion object {
        private const val fuzzyScoreThreshold = 6
    }
}
