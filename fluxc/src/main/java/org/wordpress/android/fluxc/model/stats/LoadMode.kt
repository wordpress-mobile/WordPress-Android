package org.wordpress.android.fluxc.model.stats

const val ALL_PAGE_SIZE = 100

sealed class LoadMode(open val pageSize: Int) {
    data class Paged(override val pageSize: Int, val loadMore: Boolean = false) : LoadMode(pageSize)
    object All : LoadMode(ALL_PAGE_SIZE)
}
