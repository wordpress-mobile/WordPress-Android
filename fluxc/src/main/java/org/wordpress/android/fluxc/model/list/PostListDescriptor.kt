package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.model.SiteModel

sealed class PostListDescriptor(val site: SiteModel) : ListDescriptor {
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        when (this) {
            is PostListDescriptorForRestSite -> {
                ListDescriptorUniqueIdentifier(
                        ("rest-site-post-$site.id-f${status.value}-o${order.value}-ob-${orderBy.value}" +
                                "-sq$searchQuery").hashCode()
                )
            }
            is PostListDescriptorForXmlRpcSite -> TODO()
        }
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        PostListDescriptor.calculateTypeIdentifier(site.id)
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
        val status: PostStatusForRestSite = PostStatusForRestSite.PUBLISH,
        val order: ListOrder = ListOrder.DESC,
        val orderBy: PostOrderByForRestSite = PostOrderByForRestSite.DATE,
        val searchQuery: String? = null
    ) : PostListDescriptor(site) {
        enum class PostStatusForRestSite(val value: String) {
            ANY("any"),
            DRAFT("draft"),
            PUBLISH("publish"),
            PRIVATE("private"),
            PENDING("pending"),
            FUTURE("future"),
            TRASH("trash");
        }
        enum class PostOrderByForRestSite(val value: String) {
            DATE("date"),
            LAST_MODIFIED("modified"),
            TITLE("title"),
            COMMENT_COUNT("comment_count"),
            ID("ID");
        }
    }

    class PostListDescriptorForXmlRpcSite(site: SiteModel, order: ListOrder, orderBy: PostOrderByForXmlRpcSite) :
            PostListDescriptor(site) {
        enum class PostOrderByForXmlRpcSite(val value: String) {
            DATE("date");
        }
    }
}
