package org.wordpress.android.fluxc.model.list

sealed class ListDescriptor {
    val uniqueHash: Int by lazy {
        calculateHash()
    }

    internal abstract fun calculateHash(): Int

    override fun hashCode(): Int {
        return uniqueHash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ListDescriptor

        return uniqueHash == other.uniqueHash
    }

    class PostListDescriptor @JvmOverloads constructor(
        val localSiteId: Int,
        val filter: PostListFilter = PostListFilter.ANY,
        val order: BasicListOrder = BasicListOrder.ASC,
        val searchQuery: String? = null
    ) : ListDescriptor() {
        override fun calculateHash(): Int {
            // TODO: do something better!
            return "$localSiteId-f${filter.value}-o${order.value}-s$searchQuery".hashCode()
        }
    }
}
