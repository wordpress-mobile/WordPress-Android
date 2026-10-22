package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.StatsArchivesEntry
import uniffi.wp_api.StatsArchivesPeriod
import uniffi.wp_api.StatsArchivesSummaryData
import uniffi.wp_api.WpDateString

/**
 * Covers the two pieces of the archives fetch that decide what the card and its "show all" screen
 * can show: the `max` the request carries, and how the summary is flattened into groups.
 */
@ExperimentalCoroutinesApi
class StatsDataSourceArchivesTest : BaseUnitTest() {
    @Mock
    private lateinit var wpComApiClient: WpComApiClient

    @Mock
    private lateinit var siteStore: SiteStore

    private lateinit var dataSource: StatsDataSourceImpl

    @Before
    fun setUp() {
        dataSource = StatsDataSourceImpl(wpComApiClient, siteStore)
    }

    // region max
    @Test
    fun `given a preset range, when building params, then num and max are both sent`() {
        val params = dataSource.buildArchivesParams(StatsDateRange.Preset(date = DATE, num = 7), max = 10)

        assertThat(params.period).isEqualTo(StatsArchivesPeriod.DAY)
        assertThat(params.date).isEqualTo(WpDateString(DATE))
        assertThat(params.num).isEqualTo(7u)
        assertThat(params.max).isEqualTo(10u)
        assertThat(params.summarize).isTrue()
    }

    @Test
    fun `given a custom range, when building params, then the span is sent with max`() {
        val params = dataSource.buildArchivesParams(
            StatsDateRange.Custom(startDate = START_DATE, date = DATE),
            max = 10
        )

        assertThat(params.startDate).isEqualTo(WpDateString(START_DATE))
        assertThat(params.date).isEqualTo(WpDateString(DATE))
        assertThat(params.max).isEqualTo(10u)
    }

    @Test
    fun `given max is zero, when building params, then zero is sent rather than dropped`() {
        // The server substitutes its own default of 10 for an absent `max` and only reads 0 as
        // "unlimited", so dropping the param would silently truncate the detail screen.
        val preset = dataSource.buildArchivesParams(StatsDateRange.Preset(date = DATE, num = 7), max = 0)
        val custom = dataSource.buildArchivesParams(
            StatsDateRange.Custom(startDate = START_DATE, date = DATE),
            max = 0
        )

        assertThat(preset.max).isEqualTo(0u)
        assertThat(custom.max).isEqualTo(0u)
    }

    @Test
    fun `given a negative max, when building params, then it is clamped to unlimited`() {
        val params = dataSource.buildArchivesParams(StatsDateRange.Preset(date = DATE, num = 7), max = -1)

        assertThat(params.max).isEqualTo(0u)
    }
    // endregion

    // region summary flattening
    @Test
    fun `given built-in groups and custom taxonomies, when flattening, then both become sections`() {
        val sections = sectionsOf(
            groups = mapOf("search" to listOf(entry("kotlin", views = 5uL))),
            tax = mapOf("page_condition" to listOf(entry("featured", views = 3uL)))
        )

        assertThat(sections.map { it.key })
            .containsExactlyInAnyOrder("search", ARCHIVE_SECTION_KEY_TAXONOMIES)
    }

    @Test
    fun `given several custom taxonomies, when flattening, then they share one section`() {
        // The web lists every custom taxonomy under a single "Taxonomies" row rather than one row
        // each, so their terms are ranked and totalled together.
        val sections = sectionsOf(
            tax = mapOf(
                "page_condition" to listOf(entry("featured", views = 3uL)),
                "ingredient" to listOf(entry("saffron", views = 8uL), entry("cumin", views = 1uL))
            )
        )

        val taxonomies = sections.single()
        assertThat(taxonomies.key).isEqualTo(ARCHIVE_SECTION_KEY_TAXONOMIES)
        assertThat(taxonomies.views).isEqualTo(12L)
        assertThat(taxonomies.entries.map { it.value }).containsExactly("saffron", "featured", "cumin")
    }

    @Test
    fun `given a site with no custom taxonomies, when flattening, then no taxonomies section`() {
        val sections = sectionsOf(groups = mapOf("search" to listOf(entry("kotlin", views = 5uL))))

        assertThat(sections.map { it.key }).containsExactly("search")
    }

    @Test
    fun `given taxonomy terms past the cap, when flattening, then the whole row is capped`() {
        // The cap applies to the merged row, not to each taxonomy, so one busy taxonomy cannot
        // push another's terms out of its own list.
        val sections = sectionsOf(
            tax = mapOf(
                "ingredient" to (1..80).map { entry("ingredient $it", views = it.toULong()) },
                "page_condition" to (1..80).map { entry("condition $it", views = it.toULong()) }
            )
        )

        assertThat(sections.single().entries).hasSize(MAX_ENTRIES_PER_SECTION)
    }

    @Test
    fun `given a group's entries, when flattening, then they are ranked by views`() {
        val sections = sectionsOf(
            groups = mapOf(
                "search" to listOf(
                    entry("quiet", views = 1uL),
                    entry("loud", views = 9uL),
                    entry("middling", views = 4uL)
                )
            )
        )

        assertThat(sections.single().entries.map { it.value }).containsExactly("loud", "middling", "quiet")
        assertThat(sections.single().views).isEqualTo(14L)
    }

    @Test
    fun `given more entries than the cap, when flattening, then only the top ones are kept`() {
        val entries = (1..150).map { entry("term $it", views = it.toULong()) }

        val section = sectionsOf(groups = mapOf("search" to entries)).single()

        assertThat(section.entries).hasSize(MAX_ENTRIES_PER_SECTION)
        assertThat(section.entries.first().value).isEqualTo("term 150")
        // The group's own total still counts every entry kept, not only the ones displayed.
        assertThat(section.views).isEqualTo((1..150).sum().toLong())
    }

    @Test
    fun `given entries with no term, when flattening, then they count towards nothing`() {
        // The API reports an empty site search as a blank value; the web leaves those out of the
        // group and its total, so a group's number has to match.
        val sections = sectionsOf(
            groups = mapOf(
                "search" to listOf(
                    entry("kotlin", views = 10uL),
                    entry(null, views = 33uL),
                    entry("   ", views = 25uL)
                )
            )
        )

        val section = sections.single()
        assertThat(section.entries.map { it.value }).containsExactly("kotlin")
        assertThat(section.views).isEqualTo(10L)
    }

    @Test
    fun `given a group of only blank entries, when flattening, then the group is dropped`() {
        val sections = sectionsOf(groups = mapOf("author" to listOf(entry(null, views = 48uL))))

        assertThat(sections).isEmpty()
    }

    @Test
    fun `given an entry with no views, when flattening, then it counts as zero`() {
        val sections = sectionsOf(groups = mapOf("tag" to listOf(entry("android", views = null))))

        assertThat(sections.single().views).isEqualTo(0L)
        assertThat(sections.single().entries.single().views).isEqualTo(0L)
    }

    @Test
    fun `given no summary, when flattening, then there are no sections`() {
        val missing: StatsArchivesSummaryData? = null

        assertThat(with(dataSource) { missing.toArchiveSections() }).isEmpty()
    }
    // endregion

    private fun sectionsOf(
        groups: Map<String, List<StatsArchivesEntry>> = emptyMap(),
        tax: Map<String, List<StatsArchivesEntry>> = emptyMap()
    ): List<ArchiveSectionDataItem> {
        val summary = StatsArchivesSummaryData(groups = groups, tax = tax)
        return with(dataSource) { summary.toArchiveSections() }
    }

    private fun entry(value: String?, views: ULong?) = StatsArchivesEntry(
        value = value,
        href = value?.let { "https://example.com/?s=$it" },
        views = views
    )

    companion object {
        private const val DATE = "2026-10-22"
        private const val START_DATE = "2025-11-01"
        private const val MAX_ENTRIES_PER_SECTION = 100
    }
}
