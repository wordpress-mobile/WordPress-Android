package org.wordpress.android.fluxc.model.stats

sealed class FetchMode {
    data class Paged(val pageSize: Int, val loadMore: Boolean = false) : FetchMode()
    data class Top(val limit: Int) : FetchMode()
    object All : FetchMode()
}

sealed class CacheMode {
    data class Top(val limit: Int) : CacheMode()
    object All : CacheMode()
}
