package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.DEFAULT_POST_STATUS_LIST

private const val PAGE_SIZE = 100

sealed class PostListDescriptor(
    val site: SiteModel,
    val statusList: List<PostStatus>,
    val order: ListOrder,
    val orderBy: PostListOrderBy,
    val pageSize: Int
) : ListDescriptor {
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        val statusStr = statusList.asSequence().map { it.name }.joinToString(separator = ",")
        when (this) {
            is PostListDescriptorForRestSite -> {
                ListDescriptorUniqueIdentifier(
                        ("rest-site-post-list-${site.id}-st$statusStr-o${order.value}-ob${orderBy.value}" +
                                "-sq$searchQuery").hashCode()
                )
            }
            is PostListDescriptorForXmlRpcSite -> {
                ListDescriptorUniqueIdentifier(
                        "xml-rpc-site-post-list-${site.id}-st$statusStr-o${order.value}-ob${orderBy.value}".hashCode()
                )
            }
        }
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        PostListDescriptor.calculateTypeIdentifier(site.id)
    }

    override fun hashCode(): Int {
        return uniqueIdentifier.value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PostListDescriptor
        return uniqueIdentifier == that.uniqueIdentifier
    }

    companion object {
        @JvmStatic
        fun calculateTypeIdentifier(localSiteId: Int): ListDescriptorTypeIdentifier {
            // TODO: need a better hashing algorithm, preferably a perfect hash
            return ListDescriptorTypeIdentifier("site-post-list-$localSiteId".hashCode())
        }
    }

    class PostListDescriptorForRestSite(
        site: SiteModel,
        statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
        order: ListOrder = ListOrder.DESC,
        orderBy: PostListOrderBy = PostListOrderBy.DATE,
        pageSize: Int = PAGE_SIZE,
        val searchQuery: String? = null
    ) : PostListDescriptor(site, statusList, order, orderBy, pageSize)

    class PostListDescriptorForXmlRpcSite(
        site: SiteModel,
        statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
        order: ListOrder = ListOrder.DESC,
        orderBy: PostListOrderBy = PostListOrderBy.DATE,
        pageSize: Int = PAGE_SIZE
    ) : PostListDescriptor(site, statusList, order, orderBy, pageSize)
}

enum class PostListOrderBy(val value: String) {
    DATE("date"),
    LAST_MODIFIED("modified"),
    TITLE("title"),
    COMMENT_COUNT("comment_count"),
    ID("ID");

    companion object {
        fun fromValue(value: String): PostListOrderBy? {
            return values().firstOrNull { it.value.toLowerCase() == value.toLowerCase() }
        }
    }
}
