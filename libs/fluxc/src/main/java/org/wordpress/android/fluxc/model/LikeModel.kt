package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.util.StringUtils

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
    @Column var remoteLikeId: Long = 0
    @Column var likerName: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerLogin: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerAvatarUrl: String? = null
        get() = StringUtils.notNullStr(field)
    @Column var likerSiteUrl: String? = null
        get() = StringUtils.notNullStr(field)

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId(): Int {
        return this.id
    }
}
