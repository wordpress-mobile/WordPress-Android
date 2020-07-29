package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.AuthorFilter.Everyone
import org.wordpress.android.fluxc.model.list.AuthorFilter.SpecificAuthor
import org.wordpress.android.fluxc.model.list.ListOrder.DESC
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.DEFAULT_POST_STATUS_LIST
import java.util.Locale

sealed class PostListDescriptor(
    val site: SiteModel,
    val statusList: List<PostStatus>,
    val order: ListOrder,
    val orderBy: PostListOrderBy,
    listConfig: ListConfig
) : ListDescriptor {
    override val config: ListConfig = listConfig

    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        // TODO: need a better hashing algorithm, preferably a perfect hash
        val statusStr = statusList.asSequence().map { it.name }.joinToString(separator = ",")
        when (this) {
            is PostListDescriptorForRestSite -> {
                val authorFilter: String = when (author) {
                    Everyone -> "Everyone"
                    is SpecificAuthor -> author.authorId.toString()
                }

                ListDescriptorUniqueIdentifier(
                        ("rest-site" +
                                "-post-list" +
                                "-${site.id}" +
                                "-st$statusStr" +
                                "-a$authorFilter" +
                                "-o${order.value}" +
                                "-ob${orderBy.value}" +
                                "-sq$searchQuery").hashCode()
                )
            }
            is PostListDescriptorForXmlRpcSite -> {
                ListDescriptorUniqueIdentifier(
                        ("xml-rpc-site" +
                                "-post-list" +
                                "-${site.id}" +
                                "-st$statusStr" +
                                "-o${order.value}" +
                                "-ob${orderBy.value}" +
                                "-sq$searchQuery").hashCode()
                )
            }
        }
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy { calculateTypeIdentifier(site.id) }

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
        val author: AuthorFilter = Everyone,
        order: ListOrder = DESC,
        orderBy: PostListOrderBy = DATE,
        val searchQuery: String? = null,
        config: ListConfig = ListConfig.default
    ) : PostListDescriptor(site, statusList, order, orderBy, config)

    class PostListDescriptorForXmlRpcSite(
        site: SiteModel,
        statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
        order: ListOrder = DESC,
        orderBy: PostListOrderBy = DATE,
        val searchQuery: String? = null,
        config: ListConfig = ListConfig.default
    ) : PostListDescriptor(site, statusList, order, orderBy, config)
}

enum class PostListOrderBy(val value: String) {
    DATE("date"),
    LAST_MODIFIED("modified"),
    TITLE("title"),
    COMMENT_COUNT("comment_count"),
    ID("ID");

    companion object {
        fun fromValue(value: String): PostListOrderBy? {
            return values().firstOrNull { it.value.toLowerCase(Locale.ROOT) == value.toLowerCase(Locale.ROOT) }
        }
    }
}
