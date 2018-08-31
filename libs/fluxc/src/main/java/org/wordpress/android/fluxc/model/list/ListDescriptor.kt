package org.wordpress.android.fluxc.model.list

interface ListFilter {
    val value: String
    companion object {
        fun fromValue(value: String?): ListFilter? =
                ListFilter::class.java.enumConstants?.firstOrNull { it.value == value }
    }
}

interface ListOrder {
    val value: String
    companion object {
        fun fromValue(value: String?): ListOrder? =
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
    constructor(type: Int?, filter: String?, order: String?) :
            this(ListType.fromValue(type), ListFilter.fromValue(filter), ListOrder.fromValue(order))
}
