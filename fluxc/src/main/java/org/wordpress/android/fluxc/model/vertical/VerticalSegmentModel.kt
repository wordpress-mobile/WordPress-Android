package org.wordpress.android.fluxc.model.vertical

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
class VerticalSegmentModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var title: String? = null
    @Column var subtitle: String? = null
    @Column var iconUrl: String? = null
    @Column var segmentId: Long? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
