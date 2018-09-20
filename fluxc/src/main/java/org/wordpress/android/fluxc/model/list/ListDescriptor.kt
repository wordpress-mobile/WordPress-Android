package org.wordpress.android.fluxc.model.list

data class ListDescriptorIdentifier(val identifier: Int)

sealed class ListDescriptor {
    val uniqueHash: Int by lazy {
        calculateUnique()
    }
    private val typeIdentifierHash: ListDescriptorIdentifier by lazy {
        calculateTypeIdentifier()
    }

    internal abstract fun calculateUnique(): Int
    internal abstract fun calculateTypeIdentifier(): ListDescriptorIdentifier

    override fun hashCode(): Int {
        return uniqueHash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ListDescriptor

        return uniqueHash == other.uniqueHash
    }

    fun compareIdentifier(other: ListDescriptorIdentifier): Boolean {
        return typeIdentifierHash == other
    }

    class PostListDescriptor @JvmOverloads constructor(
        val localSiteId: Int,
        val filter: PostListFilter = PostListFilter.PUBLISH,
        val order: ListOrder = ListOrder.DESC,
        val orderBy: PostOrderBy = PostOrderBy.DATE,
        val searchQuery: String? = null
    ) : ListDescriptor() {
        override fun calculateUnique(): Int {
            // TODO: do something better!
            return "post-$localSiteId-f${filter.value}-o${order.value}-ob-${orderBy.value}-sq$searchQuery".hashCode()
        }

        override fun calculateTypeIdentifier(): ListDescriptorIdentifier {
            return PostListDescriptor.typeIdentifier(localSiteId)
        }

        companion object {
            @JvmStatic
            fun typeIdentifier(localSiteId: Int): ListDescriptorIdentifier {
                // TODO: do something better!
                return ListDescriptorIdentifier("post-$localSiteId".hashCode())
            }
        }
    }
}

enum class ListOrder(val value: String) {
    ASC("ASC"),
    DESC("DESC");
}

enum class PostOrderBy(val value: String) {
    DATE("date"),
    LAST_MODIFIED("modified"),
    TITLE("title"),
    COMMENT_COUNT("comment_count"),
    ID("ID");
}
