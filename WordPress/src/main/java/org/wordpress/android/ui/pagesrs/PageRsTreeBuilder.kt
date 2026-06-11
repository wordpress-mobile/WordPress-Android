package org.wordpress.android.ui.pagesrs

internal const val MAX_INDENT_LEVEL = 3

/**
 * Assembles the final row list for a pages tab:
 *  - When [applyHierarchy] is true, [pages] are DFS-ordered as parent → children with
 *    [PageRsListItem.Real.indentLevel] capped at [MAX_INDENT_LEVEL]; children whose
 *    parent isn't visible in the set are promoted to roots so they still render.
 *  - When hierarchy is on, the pages identified by [pageOnFront] and [pageForPosts]
 *    (when set and present in [pages]) are prepended as Virtual rows and hidden from
 *    their normal sorted position.
 *  - When [applyHierarchy] is false, [pages] are wrapped as flat Real rows with
 *    indentLevel = 0 and no virtuals.
 */
internal fun buildRows(
    pages: List<PageRsUiModel>,
    applyHierarchy: Boolean,
    pageOnFront: Long,
    pageForPosts: Long
): List<PageRsListItem> {
    if (!applyHierarchy) {
        return pages.map { PageRsListItem.Real(it) }
    }
    val byId = pages.associateBy { it.remotePageId }
    val homepage = pageOnFront.takeIf { it != 0L }?.let { byId[it] }
    val postsPage = pageForPosts.takeIf { it != 0L }?.let { byId[it] }
    val hiddenIds = setOfNotNull(homepage?.remotePageId, postsPage?.remotePageId)
    val visible = pages.filterNot { it.remotePageId in hiddenIds }
    val tree = flattenToTree(visible)
    return buildList {
        homepage?.let { add(PageRsListItem.Virtual(PageRsListItem.Virtual.Kind.HOMEPAGE, it)) }
        postsPage?.let { add(PageRsListItem.Virtual(PageRsListItem.Virtual.Kind.POSTS_PAGE, it)) }
        addAll(tree)
    }
}

internal fun flattenToTree(pages: List<PageRsUiModel>): List<PageRsListItem.Real> {
    val byId = pages.associateBy { it.remotePageId }
    val childrenByParent = pages
        .filter { it.parentId != 0L && it.parentId in byId }
        .groupBy { it.parentId }
    val roots = pages.filter { it.parentId == 0L || it.parentId !in byId }

    val result = ArrayList<PageRsListItem.Real>(pages.size)
    val visited = HashSet<Long>(pages.size)
    val stack = ArrayDeque<Pair<PageRsUiModel, Int>>()
    roots.asReversed().forEach { stack.addLast(it to 0) }
    while (stack.isNotEmpty()) {
        val (page, depth) = stack.removeLast()
        if (!visited.add(page.remotePageId)) continue
        result.add(PageRsListItem.Real(page, minOf(depth, MAX_INDENT_LEVEL)))
        childrenByParent[page.remotePageId]?.asReversed()?.forEach { child ->
            stack.addLast(child to depth + 1)
        }
    }
    // Pages caught in a parent cycle (self-parented, or in a loop of parent references) are
    // unreachable from any root; append them as flat rows so corrupt data can't drop pages.
    pages.filterNot { it.remotePageId in visited }
        .mapTo(result) { PageRsListItem.Real(it) }
    return result
}
