package org.wordpress.android.ui.mysite.search

import dagger.Reusable
import org.apache.commons.text.similarity.FuzzyScore
import org.wordpress.android.R.array
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@Reusable
class SearchSuggestionsProvider @Inject constructor(
    contextProvider: ContextProvider,
    localeManagerWrapper: LocaleManagerWrapper
) {
    private val entries = contextProvider.getContext().resources.getStringArray(array.functionality_search_entries)
    private val functions: List<Functionality> = entries.map {
        val parts = it.split(DELIMITER)
        Functionality(parts[0], parts[1].split(TERMS_DELIMITER), parts[2])
    }
    private val locale = localeManagerWrapper.getLocale()
    private val fuzzyScore = FuzzyScore(locale)

    fun search(query: String) = functions.filter { f ->
        f.terms.any { term ->
            query.toLowerCase(locale).contains(term) ||
                    fuzzyScore.fuzzyScore(query, term) > fuzzyScoreThreshold
        }
    }

    companion object {
        private const val DELIMITER = "|"
        private const val TERMS_DELIMITER = ","
        private const val fuzzyScoreThreshold = 6
    }
}
