package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

@Table
@RawConstraints("UNIQUE (type)")
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    enum class ListType(val value: String) {
        POSTS_ALL("posts_all"),
        POSTS_SCHEDULED("post_scheduled"); // only added for test purposes (for now)
    }

    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var localSiteId: Int = 0
    @Column var type: String? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
