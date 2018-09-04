package org.wordpress.android.fluxc.model.list

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
    ALL("all");
}

enum class WooOrderListFilter(override val value: String) : ListFilter {
    ALL("all");
}
