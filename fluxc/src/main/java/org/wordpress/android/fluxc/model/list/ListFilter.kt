package org.wordpress.android.fluxc.model.list

/**
 * This is an interface used by `ListModel` to differentiate lists with the same `ListType`. The only requirement is
 * to have a [value] which will be saved into the DB.
 *
 * [fromValue] function will be used to convert the DB value back to a [ListFilter] instance. If there is a filtering
 * mechanism required for a `ListType` it should implement its own `enum class` extending this interface.
 * Once the enum is added, the [fromValue] function should be updated to associate the `ListType` with that enum.
 *
 * Whenever a new `ListType` is added, the compiler will warn the developer to update the [fromValue] function. If the
 * filter is unnecessary, `null` should be returned for that `ListType`.
 *
 * IMPORTANT: A catch all branch is not added to the [fromValue] function to make sure developers are reminded of
 * updating it.
 */
interface ListFilter {
    val value: String

    companion object {
        fun fromValue(listType: ListType, value: String?): ListFilter? =
                when (listType) {
                    ListType.POST -> PostListFilter.values().firstOrNull { it.value == value }
                    ListType.WOO_ORDER -> WooOrderListFilter.values().firstOrNull { it.value == value }
                }
    }
}

enum class PostListFilter(override val value: String) : ListFilter {
    ANY("any"),
    DRAFT("draft"),
    PUBLISH("publish"),
    PRIVATE("private"),
    PENDING("pending"),
    FUTURE("future"),
    TRASH("trash");
}

enum class WooOrderListFilter(override val value: String) : ListFilter {
    ALL("all");
}
