package org.wordpress.android.fluxc.model.list

/**
 * This class is used to describe a list to be used in `ListModel`, `ListStore` and network clients.
 *
 * @property type the primary and only required property. See [ListType] for more details.
 * @property localSiteId the local site id this list belongs to. This field will most likely to be utilized by many
 * lists, but it was still left as nullable since the parent of a list might be a different model all together.
 * @property filter the customizable filter that's used to further differentiate a list. See [ListFilter] for more
 * details.
 * @property order the customizable order. A basic implementation is provided with [BasicListOrder] but it can be fully
 * customized per type similar to [filter]. See [ListOrder] for more details.
 */
data class ListDescriptor(
    val type: ListType,
    val localSiteId: Int? = null,
    val filter: ListFilter? = null,
    val order: ListOrder? = null
)
