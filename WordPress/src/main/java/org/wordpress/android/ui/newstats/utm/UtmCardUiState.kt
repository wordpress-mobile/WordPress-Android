package org.wordpress.android.ui.newstats.utm

import androidx.annotation.StringRes
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.wordpress.android.R

/**
 * Represents the available UTM category combinations
 * for the dropdown selector.
 */
enum class UtmCategory(
    @StringRes val labelResId: Int,
    val keys: List<String>
) {
    SOURCE_MEDIUM(
        R.string.stats_utm_source_medium,
        listOf("utm_source", "utm_medium")
    ),
    CAMPAIGN_SOURCE_MEDIUM(
        R.string.stats_utm_campaign_source_medium,
        listOf("utm_campaign", "utm_source", "utm_medium")
    ),
    SOURCE(
        R.string.stats_utm_source,
        listOf("utm_source")
    ),
    MEDIUM(
        R.string.stats_utm_medium,
        listOf("utm_medium")
    ),
    CAMPAIGN(
        R.string.stats_utm_campaign,
        listOf("utm_campaign")
    )
}

/**
 * UI State for the UTM stats card.
 */
sealed class UtmCardUiState {
    data object Loading : UtmCardUiState()

    data class Loaded(
        val items: List<UtmUiItem>,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : UtmCardUiState()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : UtmCardUiState()
}

/**
 * A single UTM value row for display in the card.
 */
data class UtmUiItem(
    val title: String,
    val views: Long,
    val topPosts: List<UtmPostUiItem>
)

/**
 * A single post within a UTM value row.
 */
data class UtmPostUiItem(
    val title: String,
    val views: Long
)

/**
 * UI State for the UTM detail screen.
 */
sealed class UtmDetailUiState {
    data object Loading : UtmDetailUiState()

    data class Loaded(
        val items: List<UtmUiItem>,
        val maxViewsForBar: Long,
        val totalViews: Long,
        val dateRange: String,
        @StringRes val categoryLabelResId: Int
    ) : UtmDetailUiState()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : UtmDetailUiState()
}

/**
 * Formats a raw UTM name from the API
 * (e.g. `["impact","affiliate"]`) into a
 * readable slash-separated string
 * (e.g. `impact / affiliate`).
 */
internal fun formatUtmName(raw: String): String {
    if (!raw.startsWith("[")) return raw
    return try {
        val arr = JsonParser.parseString(raw)
            .asJsonArray
        arr.joinToString(" / ") { it.asString }
    } catch (_: JsonParseException) {
        raw
    }
}
