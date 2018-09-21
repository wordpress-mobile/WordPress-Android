package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.model.SiteModel

data class ListDescriptorTypeIdentifier(val value: Int)
data class ListDescriptorUniqueIdentifier(val value: Int)

interface ListDescriptor {
    val uniqueIdentifier: ListDescriptorUniqueIdentifier
    val typeIdentifier: ListDescriptorTypeIdentifier
}

enum class PostListStatus(val value: String) {
    ANY("any"),
    DRAFT("draft"),
    PUBLISH("publish"),
    PRIVATE("private"),
    PENDING("pending"),
    FUTURE("future"),
    TRASH("trash");
}

class PostListDescriptor @JvmOverloads constructor(
    val site: SiteModel,
    val status: PostListStatus = PostListStatus.PUBLISH,
    val order: ListOrder = ListOrder.DESC,
    val orderBy: PostOrderBy = PostOrderBy.DATE,
    val searchQuery: String? = null
) : ListDescriptor {
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        ListDescriptorUniqueIdentifier(("post-${site.id}-f${status.value}-o${order.value}-ob-${orderBy.value}" +
                "-sq$searchQuery").hashCode())
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        PostListDescriptor.typeIdentifier(site.id)
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
