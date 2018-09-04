package org.wordpress.android.fluxc.model.list

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
