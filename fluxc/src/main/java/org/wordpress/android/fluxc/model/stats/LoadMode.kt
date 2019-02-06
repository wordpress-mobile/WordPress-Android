package org.wordpress.android.fluxc.model.stats

const val ALL_PAGE_SIZE = 100

sealed class LoadMode(open val pageSize: Int) {
    class Paged(pageSize: Int, val loadMore: Boolean = false) : LoadMode(pageSize)
    object All : LoadMode(ALL_PAGE_SIZE)
}
