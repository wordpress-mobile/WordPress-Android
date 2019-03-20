package org.wordpress.android.fluxc.model.stats

sealed class LimitMode {
    data class Top(val limit: Int) : LimitMode()
    object All : LimitMode()
}

data class PagedMode(val pageSize: Int, val loadMore: Boolean = false)
