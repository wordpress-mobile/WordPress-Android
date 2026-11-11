package org.wordpress.android.ui.newstats.archives

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.ARCHIVE_SECTION_KEY_TAXONOMIES
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.repository.ArchiveSectionItemData
import org.wordpress.android.ui.newstats.repository.MostViewedChildData
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class ArchiveSectionMapperTest : BaseUnitTest() {
    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(R.string.stats_archives_section_categories))
            .thenReturn(CATEGORIES_LABEL)
        whenever(resourceProvider.getString(R.string.stats_archives_section_taxonomies))
            .thenReturn(TAXONOMIES_LABEL)
        whenever(resourceProvider.getString(R.string.stats_archives_section_dates)).thenReturn("Dates")
        whenever(resourceProvider.getString(R.string.stats_archives_section_error)).thenReturn("Error")
        whenever(resourceProvider.getString(R.string.stats_archives_section_aggregated))
            .thenReturn("Aggregated")
    }

    @Test
    fun `given a built-in group key, when mapped, then it is titled with its translated name`() {
        val item = section(key = "cat").toDetailItem(id = 0, resourceProvider = resourceProvider)

        assertThat(item.title).isEqualTo(CATEGORIES_LABEL)
    }

    @Test
    fun `given the merged taxonomies group, when mapped, then it is titled with its translated name`() {
        val item = section(key = ARCHIVE_SECTION_KEY_TAXONOMIES)
            .toDetailItem(id = 0, resourceProvider = resourceProvider)

        assertThat(item.title).isEqualTo(TAXONOMIES_LABEL)
    }

    @Test
    fun `given the API's less common group keys, when mapped, then they are titled as the web titles them`() {
        // These three reach the app like any other group, and read as "Date", "Err" and "Multiple"
        // without a name of their own.
        val titles = listOf("date", "err", "multiple").map { key ->
            section(key = key).toDetailItem(id = 0, resourceProvider = resourceProvider).title
        }

        assertThat(titles).containsExactly("Dates", "Error", "Aggregated")
    }

    @Test
    fun `given a group key with no translation, when mapped, then it is titled from the key`() {
        val item = section(key = "page_condition")
            .toDetailItem(id = 0, resourceProvider = resourceProvider)

        assertThat(item.title).isEqualTo("Page condition")
    }

    @Test
    fun `given entries, when mapped, then they become the row's children`() {
        val item = section(key = "cat", entryCount = 3)
            .toDetailItem(id = 4, resourceProvider = resourceProvider)

        assertThat(item.id).isEqualTo(4L)
        assertThat(item.children.map { it.name }).containsExactly("term 0", "term 1", "term 2")
        assertThat(item.children[0].url).isEqualTo("https://example.com/0")
        assertThat(item.url).isNull()
    }

    @Test
    fun `given more entries than the cap, when mapped, then only the first are kept`() {
        val item = section(key = "cat", entryCount = 5)
            .toDetailItem(id = 0, resourceProvider = resourceProvider, maxEntries = 2)

        assertThat(item.children.map { it.name }).containsExactly("term 0", "term 1")
    }

    @Test
    fun `given entries with their own children, when mapped, then the nesting is kept`() {
        val item = ArchiveSectionItemData(
            key = ARCHIVE_SECTION_KEY_TAXONOMIES,
            views = 12L,
            previousViews = 0L,
            entries = listOf(
                MostViewedChildData(
                    name = "ingredient",
                    url = null,
                    views = 9L,
                    children = listOf(
                        MostViewedChildData(name = "saffron", url = "https://example.com/saffron", views = 8L),
                        MostViewedChildData(name = "cumin", url = null, views = 1L)
                    )
                )
            )
        ).toDetailItem(id = 0, resourceProvider = resourceProvider)

        val taxonomy = item.children.single()
        assertThat(taxonomy.name).isEqualTo("ingredient")
        assertThat(taxonomy.children.map { it.name }).containsExactly("saffron", "cumin")
        assertThat(taxonomy.children[0].url).isEqualTo("https://example.com/saffron")
    }

    @Test
    fun `given nested entries past the cap, when mapped, then the cap applies at both levels`() {
        val item = ArchiveSectionItemData(
            key = ARCHIVE_SECTION_KEY_TAXONOMIES,
            views = 12L,
            previousViews = 0L,
            entries = (0 until 5).map { index ->
                MostViewedChildData(
                    name = "taxonomy $index",
                    url = null,
                    views = 1L,
                    children = (0 until 5).map { term ->
                        MostViewedChildData(name = "term $term", url = null, views = 1L)
                    }
                )
            }
        ).toDetailItem(id = 0, resourceProvider = resourceProvider, maxEntries = 2)

        assertThat(item.children.map { it.name }).containsExactly("taxonomy 0", "taxonomy 1")
        assertThat(item.children[0].children.map { it.name }).containsExactly("term 0", "term 1")
    }

    @Test
    fun `given a group that grew, when mapped, then its change is positive`() {
        val item = section(key = "cat", views = 150L, previousViews = 100L)
            .toDetailItem(id = 0, resourceProvider = resourceProvider)

        assertThat(item.views).isEqualTo(150L)
        assertThat(item.change).isEqualTo(MostViewedChange.Positive(50L, 50.0))
    }

    private fun section(
        key: String,
        views: Long = 10L,
        previousViews: Long = 0L,
        entryCount: Int = 1
    ) = ArchiveSectionItemData(
        key = key,
        views = views,
        previousViews = previousViews,
        entries = (0 until entryCount).map { index ->
            MostViewedChildData(
                name = "term $index",
                url = "https://example.com/$index",
                views = (entryCount - index).toLong()
            )
        }
    )

    companion object {
        private const val CATEGORIES_LABEL = "Categories"
        private const val TAXONOMIES_LABEL = "Taxonomies"
    }
}
