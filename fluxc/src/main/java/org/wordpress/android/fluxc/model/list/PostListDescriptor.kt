package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.model.SiteModel

sealed class PostListDescriptor(val site: SiteModel) : ListDescriptor {
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        when (this) {
            is PostListDescriptorForRestSite -> {
                ListDescriptorUniqueIdentifier(
                        ("rest-site-post-list-$site.id-st${status.value}-o${order.value}-ob${orderBy.value}" +
                                "-sq$searchQuery").hashCode()
                )
            }
            is PostListDescriptorForXmlRpcSite -> {
                ListDescriptorUniqueIdentifier(
                        "xml-rpc-site-post-list-${site.id}-o${order.value}-ob${orderBy.value}".hashCode()
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
        val status: PostStatusForRestSite = PostStatusForRestSite.PUBLISH,
        val order: ListOrder = ListOrder.DESC,
        val orderBy: PostListOrderBy = PostListOrderBy.DATE,
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

            companion object {
                fun fromValue(value: String): PostStatusForRestSite? {
                    return values().firstOrNull { it.value.toLowerCase() == value.toLowerCase() }
                }
            }
        }
    }

    class PostListDescriptorForXmlRpcSite(
        site: SiteModel,
        val order: ListOrder = ListOrder.DESC,
        val orderBy: PostListOrderBy = PostListOrderBy.DATE
    ) : PostListDescriptor(site)
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
