package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

/**
 * Rows of this table represent that a valid xpost referencing [targetSiteId]
 * may be added to [sourceSiteId].
 */
@Table(name = "XPosts")
@RawConstraints(
        "FOREIGN KEY(SOURCE_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "FOREIGN KEY(TARGET_SITE_ID) REFERENCES XPostSites(BLOG_ID) ON DELETE CASCADE",
        "UNIQUE (SOURCE_SITE_ID, TARGET_SITE_ID) ON CONFLICT IGNORE"
)
data class XPostModel(
    @PrimaryKey @Column private var id: Int,
    @Column var sourceSiteId: Int,
    @Column var targetSiteId: Int
) : Identifiable {
    constructor() : this(0, 0, 0)

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId(): Int = id
}
