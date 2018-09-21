package org.wordpress.android.fluxc.model.list

enum class ListOrder(val value: String) {
    ASC("ASC"),
    DESC("DESC");
    companion object {
        fun fromValue(value: String): ListOrder? {
            return values().firstOrNull { it.value.toLowerCase() == value.toLowerCase() }
        }
    }
}
