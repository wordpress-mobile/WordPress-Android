package org.wordpress.android.ui.pagesrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentListDensity

private const val SITE_EDITOR_TITLE = "Homepage"
private const val SITE_EDITOR_SUBTITLE = "Opens in the web editor"

internal class PageRsRedesignedRowMappingTest {
    @Test
    fun `a real page maps onto the shared row state`() {
        val row = realRow(
            page(
                remotePageId = 7L,
                title = "About",
                excerpt = "Who we are",
                date = "Mar 3, 2025"
            )
        )

        val state = row.map()

        assertThat(state.id).isEqualTo(7L)
        assertThat(state.title).isEqualTo("About")
        assertThat(state.excerpt).isEqualTo("Who we are")
        assertThat(state.dateLabel).isEqualTo("Mar 3, 2025")
        assertThat(state.badges).isEmpty()
    }

    @Test
    fun `the homepage row leads with its label as a badge`() {
        val row = PageRsListItem.Virtual(
            kind = PageRsListItem.Virtual.Kind.HOMEPAGE,
            page = page(badges = listOf(R.string.post_status_post_private))
        )

        val state = row.map()

        // Ahead of the page's own badges: it says what the row *is*, not how it is configured.
        assertThat(state.badges)
            .containsExactly(R.string.site_settings_homepage, R.string.post_status_post_private)
    }

    @Test
    fun `the site editor row shows the supplied text and no date`() {
        val row = PageRsListItem.Virtual(
            kind = PageRsListItem.Virtual.Kind.SITE_EDITOR,
            page = page(remotePageId = SITE_EDITOR_PAGE_ID, title = "", excerpt = "", date = "")
        )

        val state = row.map()

        assertThat(state.title).isEqualTo(SITE_EDITOR_TITLE)
        assertThat(state.excerpt).isEqualTo(SITE_EDITOR_SUBTITLE)
        assertThat(state.dateLabel).isEmpty()
        // It stands for the theme's homepage rather than a page, so it carries no label badge.
        assertThat(state.badges).isEmpty()
    }

    @Test
    fun `the search-mode status label rides along as a badge`() {
        val row = realRow(page(statusLabelResId = R.string.post_status_draft))

        assertThat(row.map().badges).containsExactly(R.string.post_status_draft)
    }

    @Test
    fun `a row waits for a featured image it has not resolved yet`() {
        val row = realRow(page(featuredImageId = 42L))

        assertThat(row.map().isImagePending).isTrue()
    }

    @Test
    fun `a row whose featured image could not be resolved stops waiting`() {
        val row = realRow(page(featuredImageId = 42L, isFeaturedImageUnresolvable = true))

        assertThat(row.map().isImagePending).isFalse()
    }

    @Test
    fun `a top-level page with a featured image is image-led`() {
        val row = realRow(page(featuredImageId = 42L))

        assertThat(row.isHeroRow(ContentListDensity.COMFORTABLE)).isTrue()
    }

    @Test
    fun `an indented page is never image-led`() {
        val row = realRow(page(featuredImageId = 42L), indentLevel = 1)

        assertThat(row.isHeroRow(ContentListDensity.COMFORTABLE)).isFalse()
    }

    @Test
    fun `a condensed list draws no image-led rows`() {
        val row = realRow(page(featuredImageId = 42L))

        assertThat(row.isHeroRow(ContentListDensity.CONDENSED)).isFalse()
    }

    @Test
    fun `a virtual row is never image-led`() {
        val row = PageRsListItem.Virtual(
            kind = PageRsListItem.Virtual.Kind.HOMEPAGE,
            page = page(featuredImageId = 42L)
        )

        assertThat(row.isHeroRow(ContentListDensity.COMFORTABLE)).isFalse()
    }

    @Test
    fun `a page whose image could not be resolved falls back to the compact shape`() {
        val row = realRow(page(featuredImageId = 42L, isFeaturedImageUnresolvable = true))

        assertThat(row.isHeroRow(ContentListDensity.COMFORTABLE)).isFalse()
    }

    private fun PageRsListItem.map() = toContentListRowUiState(
        siteEditorTitle = SITE_EDITOR_TITLE,
        siteEditorSubtitle = SITE_EDITOR_SUBTITLE
    )

    private fun realRow(page: PageRsUiModel, indentLevel: Int = 0) =
        PageRsListItem.Real(page, indentLevel)

    @Suppress("LongParameterList")
    private fun page(
        remotePageId: Long = 1L,
        title: String = "Title",
        excerpt: String = "Excerpt",
        date: String = "Jan 1, 2025",
        featuredImageId: Long = 0L,
        isFeaturedImageUnresolvable: Boolean = false,
        statusLabelResId: Int = 0,
        badges: List<Int> = emptyList()
    ) = PageRsUiModel(
        remotePageId = remotePageId,
        title = title,
        excerpt = excerpt,
        date = date,
        featuredImageId = featuredImageId,
        isFeaturedImageUnresolvable = isFeaturedImageUnresolvable,
        statusLabelResId = statusLabelResId,
        badges = badges
    )
}
