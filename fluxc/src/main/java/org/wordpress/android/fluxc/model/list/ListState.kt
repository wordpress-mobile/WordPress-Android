package org.wordpress.android.fluxc.model.list

/**
 * This is an enum used by `ListStore` to manage the state of a [ListModel]. It'll be saved to `ListModelTable`.
 *
 * IMPORTANT: Because the values are stored in the DB, in case of a change to the enum values a migration needs to
 * be added!
 */
enum class ListState(val value: Int) {
    NEEDS_REFRESH(0),
    CAN_LOAD_MORE(1),
    FETCHED(2),
    FETCHING_FIRST_PAGE(3),
    LOADING_MORE(4),
    ERROR(5);

    fun canLoadMore() = this == CAN_LOAD_MORE

    fun isFetchingFirstPage() = this == FETCHING_FIRST_PAGE

    fun isLoadingMore() = this == LOADING_MORE

    companion object {
        val defaultState = NEEDS_REFRESH
        val notExpiredStates = setOf(CAN_LOAD_MORE, FETCHED)
    }
}
