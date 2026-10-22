package org.wordpress.android.ui.newstats.archives

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.ARCHIVE_SECTION_KEY_TAXONOMIES
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChildItem
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.repository.ArchiveSectionItemData
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

/**
 * Maps an archive group onto the shared top-list shape: the group becomes the row, and the
 * archive pages in it the children revealed when the row is expanded. Shared by the card
 * ([ArchivesViewModel]) and its "show all" screen, which differ only in [maxEntries].
 */
internal fun ArchiveSectionItemData.toDetailItem(
    id: Long,
    resourceProvider: ResourceProvider,
    maxEntries: Int = Int.MAX_VALUE
) = MostViewedDetailItem(
    id = id,
    title = sectionTitle(resourceProvider),
    views = views,
    change = MostViewedChange.fromChange(viewsChange, viewsChangePercent),
    children = entries.take(maxEntries).map { entry ->
        MostViewedChildItem(name = entry.name, url = entry.url, views = entry.views)
    }
)

/**
 * The label a group is listed under. The API names its built-in groups with short keys that read
 * poorly on their own ("cat", "author"), so those get a translated name, as does the one group
 * holding every custom taxonomy. A key with no translation -- one of the rarer built-in ones the
 * API can still send -- is spelled out from itself ("page_condition" -> "Page condition") rather
 * than shown raw.
 */
private fun ArchiveSectionItemData.sectionTitle(resourceProvider: ResourceProvider): String {
    val titleResId = when (key) {
        SECTION_KEY_SEARCH -> R.string.stats_archives_section_search
        SECTION_KEY_CATEGORY -> R.string.stats_archives_section_categories
        SECTION_KEY_TAG -> R.string.stats_archives_section_tags
        SECTION_KEY_AUTHOR -> R.string.stats_archives_section_authors
        SECTION_KEY_OTHER -> R.string.stats_archives_section_other
        ARCHIVE_SECTION_KEY_TAXONOMIES -> R.string.stats_archives_section_taxonomies
        else -> null
    }
    return titleResId?.let { resourceProvider.getString(it) } ?: key.humanizeSlug()
}

private fun String.humanizeSlug(): String = replace('_', ' ')
    .replace('-', ' ')
    .replaceFirstChar { it.titlecase(Locale.getDefault()) }

private const val SECTION_KEY_SEARCH = "search"
private const val SECTION_KEY_CATEGORY = "cat"
private const val SECTION_KEY_TAG = "tag"
private const val SECTION_KEY_AUTHOR = "author"
private const val SECTION_KEY_OTHER = "other"
