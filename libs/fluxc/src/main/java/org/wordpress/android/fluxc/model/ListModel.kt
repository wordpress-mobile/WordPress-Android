package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.ListModel.State.CAN_LOAD_MORE

@Table
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE(LOCAL_SITE_ID, TYPE)"
)
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    enum class ListType(val value: String) {
        POSTS_ALL("posts_all"),
        POSTS_SCHEDULED("post_scheduled"); // only added for test purposes (for now)
    }

    enum class State {
        CAN_LOAD_MORE,
        FETCHED,
        FETCHING_FIRST_PAGE,
        LOADING_MORE,
        ERROR;

        fun canLoadMore(): Boolean {
            return this == CAN_LOAD_MORE
        }

        fun isFetchingFirstPage(): Boolean {
            return this == FETCHING_FIRST_PAGE
        }

        fun isLoadingMore(): Boolean {
            return this == LOADING_MORE
        }
    }

    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var localSiteId: Int = 0
    @Column var type: String? = null
    @Column var state: State = CAN_LOAD_MORE

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
