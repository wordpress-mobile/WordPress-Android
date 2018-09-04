package org.wordpress.android.fluxc.model.list

enum class ListType(val value: Int) {
    POST(0),
    WOO_ORDER(1);

    companion object {
        // If the type is missing we want the app to crash so we can fix it immediately
        fun fromValue(value: Int?): ListType = ListType.values().firstOrNull { it.value == value }!!
    }
}
