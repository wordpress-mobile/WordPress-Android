package org.wordpress.android.fluxc.model.list

/**
 * This is an interface used by `ListModel` to indicate the order of a list. The only requirement is
 * to have a [value] which will be saved into the DB.
 *
 * [fromValue] function will be used to convert the DB value back to a [ListOrder] instance.
 * Once the enum is added, the [fromValue] function should be updated to associate the `ListType` with that enum.
 *
 * A [BasicListOrder] is provided, but if a different order is required per type, a similar approach to `ListFilter`
 * could be utilized. [fromValue] function requires the list type for this reason.
 */
interface ListOrder {
    val value: String

    companion object {
        // TODO: use `listType` to return different `ListOrder` instances depending on type similar to `ListFilter`
        @Suppress("unused_parameter")
        fun fromValue(listType: ListType, value: String?): ListOrder? =
                BasicListOrder.values().firstOrNull { it.value == value }
    }
}

enum class BasicListOrder(override val value: String) : ListOrder {
    ASC("asc"),
    DESC("desc");
}
