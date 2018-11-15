package org.wordpress.android.fluxc.model.list

private const val DB_PAGE_SIZE = 10
private const val INITIAL_LOAD_SIZE = 20
private const val NETWORK_PAGE_SIZE = 60
private const val PRE_FETCH_DISTANCE = DB_PAGE_SIZE * 3

class ListConfig(val networkPageSize: Int, val initialLoadSize: Int, val dbPageSize: Int, val prefetchDistance: Int) {
    companion object {
        val default = ListConfig(
                networkPageSize = NETWORK_PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                dbPageSize = DB_PAGE_SIZE,
                prefetchDistance = PRE_FETCH_DISTANCE
        )
    }
}
