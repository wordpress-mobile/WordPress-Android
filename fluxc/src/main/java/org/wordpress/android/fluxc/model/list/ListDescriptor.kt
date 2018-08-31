package org.wordpress.android.fluxc.model.list

interface ListFilter {
    val value: Int

    companion object {
        fun fromValue(value: Int?): ListFilter? =
                ListFilter::class.java.enumConstants?.firstOrNull { it.value == value }
    }
}

interface ListOrder {
    val value: Int

    companion object {
        fun fromValue(value: Int?): ListOrder? =
                ListOrder::class.java.enumConstants?.firstOrNull { it.value == value }
    }
}

enum class ListType(val value: Int) {
    POST(0),
    WOO_ORDER(1);

    companion object {
        // If the type is missing we want the app to crash so we can fix it immediately
        fun fromValue(value: Int?): ListType = ListType.values().firstOrNull { it.value == value }!!
    }
}

data class ListDescriptor(val type: ListType, val filter: ListFilter? = null, val order: ListOrder? = null) {
    constructor(type: Int?, filter: Int?, order: Int?) :
            this(ListType.fromValue(type), ListFilter.fromValue(filter), ListOrder.fromValue(order))
}
