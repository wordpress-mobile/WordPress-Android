package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.util.StringUtils
import java.util.Date

@Table
class LikeModel : Identifiable {
    enum class LikeType(val typeName: String) {
        POST_LIKE("post-like"),
        COMMENT_LIKE("comment-like");

        fun fromTypeName(name: String): LikeType {
            return values().firstOrNull { it.typeName == name }
                    ?: throw IllegalArgumentException("LikesType unexpected value $name")
        }
    }

    @PrimaryKey
    @Column private var id = 0

    @Column var type: String = POST_LIKE.typeName
    @Column var remoteSiteId: Long = 0
    @Column var remoteItemId: Long = 0 // Either Post remote id or Comment remote id
    @Column var likerId: Long = 0
    @Column var likerName: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerLogin: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerAvatarUrl: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerBio: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerSiteId: Long = 0
    @Column var likerSiteUrl: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var preferredBlogId: Long = 0
    @Column var preferredBlogName: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var preferredBlogUrl: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var preferredBlogBlavatarUrl: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var dateLiked: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var timestampFetched: Long = Date().time

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId(): Int {
        return this.id
    }

    @Suppress("ComplexMethod")
    fun isEqual(otherLike: LikeModel): Boolean {
        return type == otherLike.type &&
            remoteSiteId == otherLike.remoteSiteId &&
            remoteItemId == otherLike.remoteItemId &&
            likerId == otherLike.likerId &&
            likerName == otherLike.likerName &&
            likerLogin == otherLike.likerLogin &&
            likerAvatarUrl == otherLike.likerAvatarUrl &&
            likerBio == otherLike.likerBio &&
            likerSiteId == otherLike.likerSiteId &&
            likerSiteUrl == otherLike.likerSiteUrl &&
            preferredBlogId == otherLike.preferredBlogId &&
            preferredBlogName == otherLike.preferredBlogName &&
            preferredBlogUrl == otherLike.preferredBlogUrl &&
            preferredBlogBlavatarUrl == otherLike.preferredBlogBlavatarUrl &&
            dateLiked == otherLike.dateLiked
    }

    companion object {
        const val TIMESTAMP_THRESHOLD = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    }
}
