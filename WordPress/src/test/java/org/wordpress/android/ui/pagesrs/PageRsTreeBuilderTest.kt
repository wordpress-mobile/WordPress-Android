package org.wordpress.android.ui.pagesrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PageRsTreeBuilderTest {
    @Test
    fun `flat input with no parents stays flat`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 0L, pageForPosts = 0L)

        assertThat(rows).hasSize(3)
        assertThat(rows.map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L, 3L)
        assertThat(rows.map { (it as PageRsListItem.Real).indentLevel })
            .containsOnly(0)
    }

    @Test
    fun `single tree is DFS-ordered with depth indents`() {
        // 1 (root) → 2 (child) → 3 (grandchild); 4 sibling of 2
        val pages = listOf(
            page(1),
            page(2, parentId = 1),
            page(3, parentId = 2),
            page(4, parentId = 1),
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.page.remotePageId }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(rows.map { it.indentLevel }).containsExactly(0, 1, 2, 1)
    }

    @Test
    fun `multiple trees preserve root order`() {
        val pages = listOf(
            page(10),
            page(11, parentId = 10),
            page(20),
            page(21, parentId = 20),
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.page.remotePageId }).containsExactly(10L, 11L, 20L, 21L)
        assertThat(rows.map { it.indentLevel }).containsExactly(0, 1, 0, 1)
    }

    @Test
    fun `child with missing parent is promoted to root`() {
        val pages = listOf(
            page(1),
            page(2, parentId = 999), // parent not present
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.page.remotePageId }).containsExactly(1L, 2L)
        assertThat(rows.map { it.indentLevel }).containsExactly(0, 0)
    }

    @Test
    fun `indent is capped at MAX_INDENT_LEVEL`() {
        // 1 → 2 → 3 → 4 → 5 (depth 4 caps at 3)
        val pages = listOf(
            page(1),
            page(2, parentId = 1),
            page(3, parentId = 2),
            page(4, parentId = 3),
            page(5, parentId = 4),
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.indentLevel })
            .containsExactly(0, 1, 2, 3, MAX_INDENT_LEVEL)
    }

    @Test
    fun `applyHierarchy false skips tree and virtuals`() {
        val pages = listOf(
            page(1),
            page(2, parentId = 1),
        )

        val rows = buildRows(pages, applyHierarchy = false, pageOnFront = 1L, pageForPosts = 0L)

        assertThat(rows).allMatch { it is PageRsListItem.Real }
        assertThat(rows.map { (it as PageRsListItem.Real).indentLevel }).containsOnly(0)
    }

    @Test
    fun `pageOnFront prepends Virtual HOMEPAGE and hides real row`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 2L, pageForPosts = 0L)

        assertThat(rows).hasSize(3)
        assertThat(rows[0]).isInstanceOfSatisfying(PageRsListItem.Virtual::class.java) { virtual ->
            assertThat(virtual.kind).isEqualTo(PageRsListItem.Virtual.Kind.HOMEPAGE)
            assertThat(virtual.page.remotePageId).isEqualTo(2L)
        }
        assertThat(rows.drop(1).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 3L)
    }

    @Test
    fun `pageForPosts prepends Virtual POSTS_PAGE and hides real row`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 0L, pageForPosts = 3L)

        assertThat(rows[0]).isInstanceOfSatisfying(PageRsListItem.Virtual::class.java) { virtual ->
            assertThat(virtual.kind).isEqualTo(PageRsListItem.Virtual.Kind.POSTS_PAGE)
            assertThat(virtual.page.remotePageId).isEqualTo(3L)
        }
        assertThat(rows.drop(1).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `both virtuals are prepended in order homepage then posts page`() {
        val pages = listOf(page(1), page(2), page(3), page(4))

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 2L, pageForPosts = 3L)

        assertThat((rows[0] as PageRsListItem.Virtual).kind)
            .isEqualTo(PageRsListItem.Virtual.Kind.HOMEPAGE)
        assertThat((rows[0] as PageRsListItem.Virtual).page.remotePageId).isEqualTo(2L)
        assertThat((rows[1] as PageRsListItem.Virtual).kind)
            .isEqualTo(PageRsListItem.Virtual.Kind.POSTS_PAGE)
        assertThat((rows[1] as PageRsListItem.Virtual).page.remotePageId).isEqualTo(3L)
        assertThat(rows.drop(2).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 4L)
    }

    @Test
    fun `virtual is not injected when real page not in loaded set`() {
        val pages = listOf(page(1), page(2))

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 999L, pageForPosts = 0L)

        assertThat(rows).allMatch { it is PageRsListItem.Real }
        assertThat(rows.map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `virtual underlying page keeps its children visible in the tree`() {
        // page 2 is homepage; its child 3 should still render under the (hidden) parent's slot
        // but since the parent is hidden, child 3 is promoted to root.
        val pages = listOf(
            page(1),
            page(2),
            page(3, parentId = 2),
        )

        val rows = buildRows(pages, applyHierarchy = true, pageOnFront = 2L, pageForPosts = 0L)

        // Virtual homepage first, then page 3 (orphaned by hidden parent) and page 1 as roots.
        assertThat(rows[0]).isInstanceOf(PageRsListItem.Virtual::class.java)
        val realIds = rows.drop(1).map { (it as PageRsListItem.Real).page.remotePageId }
        assertThat(realIds).containsExactly(1L, 3L)
        assertThat(rows.drop(1).map { (it as PageRsListItem.Real).indentLevel })
            .containsOnly(0)
    }

    @Test
    fun `showSiteEditorHomepage prepends a single SITE_EDITOR virtual`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(
            pages,
            applyHierarchy = true,
            pageOnFront = 0L,
            pageForPosts = 0L,
            showSiteEditorHomepage = true
        )

        assertThat(rows).hasSize(4)
        assertThat(rows[0]).isInstanceOfSatisfying(PageRsListItem.Virtual::class.java) { virtual ->
            assertThat(virtual.kind).isEqualTo(PageRsListItem.Virtual.Kind.SITE_EDITOR)
            assertThat(virtual.page.remotePageId).isEqualTo(SITE_EDITOR_PAGE_ID)
        }
        assertThat(rows.drop(1).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `showSiteEditorHomepage replaces the static HOMEPAGE row and hides its page`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(
            pages,
            applyHierarchy = true,
            pageOnFront = 2L,
            pageForPosts = 0L,
            showSiteEditorHomepage = true
        )

        // SITE_EDITOR is shown instead of the static HOMEPAGE virtual, and page 2 is hidden.
        assertThat(rows).hasSize(3)
        assertThat((rows[0] as PageRsListItem.Virtual).kind)
            .isEqualTo(PageRsListItem.Virtual.Kind.SITE_EDITOR)
        assertThat(rows.drop(1).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 3L)
    }

    @Test
    fun `showSiteEditorHomepage still shows the POSTS_PAGE virtual after SITE_EDITOR`() {
        val pages = listOf(page(1), page(2), page(3))

        val rows = buildRows(
            pages,
            applyHierarchy = true,
            pageOnFront = 0L,
            pageForPosts = 3L,
            showSiteEditorHomepage = true
        )

        assertThat((rows[0] as PageRsListItem.Virtual).kind)
            .isEqualTo(PageRsListItem.Virtual.Kind.SITE_EDITOR)
        assertThat((rows[1] as PageRsListItem.Virtual).kind)
            .isEqualTo(PageRsListItem.Virtual.Kind.POSTS_PAGE)
        assertThat((rows[1] as PageRsListItem.Virtual).page.remotePageId).isEqualTo(3L)
        assertThat(rows.drop(2).map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `showSiteEditorHomepage is ignored when applyHierarchy is false`() {
        val pages = listOf(page(1), page(2))

        val rows = buildRows(
            pages,
            applyHierarchy = false,
            pageOnFront = 0L,
            pageForPosts = 0L,
            showSiteEditorHomepage = true
        )

        assertThat(rows).allMatch { it is PageRsListItem.Real }
        assertThat(rows.map { (it as PageRsListItem.Real).page.remotePageId })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `self-parented page is appended as a flat row instead of dropped`() {
        val pages = listOf(
            page(1),
            page(2, parentId = 2),
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.page.remotePageId }).containsExactly(1L, 2L)
        assertThat(rows.map { it.indentLevel }).containsOnly(0)
    }

    @Test
    fun `pages in a parent cycle are appended as flat rows instead of dropped`() {
        // 2 and 3 parent each other; 4's parent is inside the cycle
        val pages = listOf(
            page(1),
            page(2, parentId = 3),
            page(3, parentId = 2),
            page(4, parentId = 2),
        )

        val rows = flattenToTree(pages)

        assertThat(rows.map { it.page.remotePageId }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(rows.map { it.indentLevel }).containsOnly(0)
    }

    private fun page(id: Long, parentId: Long = 0L) = PageRsUiModel(
        remotePageId = id,
        parentId = parentId,
        title = "Page $id",
        excerpt = "",
        date = ""
    )
}
