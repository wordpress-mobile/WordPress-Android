package org.wordpress.android.fluxc.model.list

data class ListDescriptor(
    val type: ListType,
    val localSiteId: Int? = null,
    val filter: ListFilter? = null,
    val order: ListOrder? = null
) {
    constructor(type: Int?, localSiteId: Int?, filter: String?, order: String?) :
            this(ListType.fromValue(type), localSiteId, filter, order)

    constructor(type: ListType, localSiteId: Int?, filter: String?, order: String?) :
            this(type, localSiteId, ListFilter.fromValue(type, filter), ListOrder.fromValue(type, order))
}
