package org.wordpress.android.fluxc.model.list

data class ListDescriptorIdentifier(val identifier: Int)

interface ListDescriptor {
    fun calculateUnique(): Int
    fun calculateTypeIdentifier(): ListDescriptorIdentifier

    fun compareIdentifier(other: ListDescriptorIdentifier): Boolean {
        return calculateTypeIdentifier() == other
    }
}

class PostListDescriptor @JvmOverloads constructor(
    val localSiteId: Int,
    val filter: PostListFilter = PostListFilter.PUBLISH,
    val order: ListOrder = ListOrder.DESC,
    val orderBy: PostOrderBy = PostOrderBy.DATE,
    val searchQuery: String? = null
) : ListDescriptor {
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
