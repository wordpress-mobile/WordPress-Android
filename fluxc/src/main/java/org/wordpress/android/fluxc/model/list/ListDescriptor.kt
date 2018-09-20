package org.wordpress.android.fluxc.model.list

data class ListDescriptorTypeIdentifier(val value: Int)
data class ListDescriptorUniqueIdentifier(val value: Int)

interface ListDescriptor {
    val uniqueIdentifier: ListDescriptorUniqueIdentifier
    val typeIdentifier: ListDescriptorTypeIdentifier
}

class PostListDescriptor @JvmOverloads constructor(
    val localSiteId: Int,
    val filter: PostListFilter = PostListFilter.PUBLISH,
    val order: ListOrder = ListOrder.DESC,
    val orderBy: PostOrderBy = PostOrderBy.DATE,
    val searchQuery: String? = null
) : ListDescriptor {
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        ListDescriptorUniqueIdentifier(("post-$localSiteId-f${filter.value}-o${order.value}-ob-${orderBy.value}" +
                "-sq$searchQuery").hashCode())
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        PostListDescriptor.typeIdentifier(localSiteId)
    }

    companion object {
        @JvmStatic
        fun typeIdentifier(localSiteId: Int): ListDescriptorTypeIdentifier {
            // TODO: need a better hashing algorithm, preferably a perfect hash
            return ListDescriptorTypeIdentifier("post-$localSiteId".hashCode())
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
