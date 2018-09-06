package org.wordpress.android.fluxc.model.list

enum class ListState(val value: Int) {
    NEEDS_REFRESH(0),
    CAN_LOAD_MORE(1),
    FETCHED(2),
    FETCHING_FIRST_PAGE(3),
    LOADING_MORE(4),
    ERROR(5);

    companion object {
        val defaultState = ListState.NEEDS_REFRESH
    }
}
