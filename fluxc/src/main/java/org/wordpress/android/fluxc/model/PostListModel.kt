package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

@Table
@RawConstraints(
        "FOREIGN KEY(LIST_ID) REFERENCES ListModel(_id) ON DELETE CASCADE",
        "UNIQUE(LIST_ID, REMOTE_POST_ID)"
)
class PostListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var listId: Int = 0
    @Column var remotePostId: Long = 0

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
