package org.wordpress.android.ui.newstats.archives

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.ARCHIVE_SECTION_KEY_TAXONOMIES
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChildItem
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.repository.ArchiveSectionItemData
import org.wordpress.android.ui.newstats.repository.MostViewedChildData
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

/**
 * Maps an archive group onto the shared top-list shape: the group becomes the row, and the
 * archive pages in it the children revealed when the row is expanded. Shared by the card
 * ([ArchivesViewModel]) and its "show all" screen, which differ only in [maxEntries].
 *
 * The "Taxonomies" group is a level deeper than the others -- one child per custom taxonomy, its
 * terms nested under that -- so [maxEntries] is applied at both levels: a card that lists ten
 * taxonomies lists ten terms under each of them.
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
    children = entries.take(maxEntries).map { entry -> entry.toChildItem(maxEntries) }
)

private fun MostViewedChildData.toChildItem(maxEntries: Int): MostViewedChildItem = MostViewedChildItem(
    name = name,
    url = url,
    views = views,
    children = children.take(maxEntries).map { child -> child.toChildItem(maxEntries) }
)

/**
 * The label a group is listed under. The API names its groups with short keys that read poorly on
 * their own ("cat", "err"), so each one it can send gets a translated name, matching the names the
 * web lists them under. A key with no translation -- one the API gains after this was written -- is
 * spelled out from itself ("page_condition" -> "Page condition") rather than shown raw.
 *
 * The API's `home` group is deliberately absent: the request excludes it, as the web's does. See
 * `StatsDataSourceImpl.buildArchivesParams`.
 */
private fun ArchiveSectionItemData.sectionTitle(resourceProvider: ResourceProvider): String {
    val titleResId = when (key) {
        SECTION_KEY_SEARCH -> R.string.stats_archives_section_search
        SECTION_KEY_CATEGORY -> R.string.stats_archives_section_categories
        SECTION_KEY_TAG -> R.string.stats_archives_section_tags
        SECTION_KEY_AUTHOR -> R.string.stats_archives_section_authors
        SECTION_KEY_DATE -> R.string.stats_archives_section_dates
        SECTION_KEY_ERROR -> R.string.stats_archives_section_error
        SECTION_KEY_MULTIPLE -> R.string.stats_archives_section_aggregated
        SECTION_KEY_OTHER -> R.string.stats_archives_section_other
        ARCHIVE_SECTION_KEY_TAXONOMIES -> R.string.stats_archives_section_taxonomies
        else -> null
    }
    return titleResId?.let { resourceProvider.getString(it) } ?: key.humanizeSlug()
}

// ROOT, not the device locale: the input is an ASCII API key, and Turkish would title-case its
// leading "i" to a dotted "I".
private fun String.humanizeSlug(): String = replace('_', ' ')
    .replace('-', ' ')
    .replaceFirstChar { it.titlecase(Locale.ROOT) }

private const val SECTION_KEY_SEARCH = "search"
private const val SECTION_KEY_CATEGORY = "cat"
private const val SECTION_KEY_TAG = "tag"
private const val SECTION_KEY_AUTHOR = "author"
private const val SECTION_KEY_DATE = "date"
private const val SECTION_KEY_ERROR = "err"
private const val SECTION_KEY_MULTIPLE = "multiple"
private const val SECTION_KEY_OTHER = "other"
