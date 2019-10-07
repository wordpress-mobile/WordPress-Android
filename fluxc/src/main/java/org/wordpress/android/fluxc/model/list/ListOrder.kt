package org.wordpress.android.fluxc.model.list

import java.util.Locale

enum class ListOrder(val value: String) {
    ASC("ASC"),
    DESC("DESC");
    companion object {
        fun fromValue(value: String): ListOrder? {
            return values().firstOrNull { it.value.toLowerCase(Locale.ROOT) == value.toLowerCase(Locale.ROOT) }
        }
    }
}
