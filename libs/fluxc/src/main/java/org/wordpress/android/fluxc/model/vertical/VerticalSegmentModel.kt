package org.wordpress.android.fluxc.model.vertical

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
// TODO: Do we need any constraints?
class VerticalSegmentModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    // TODO: Delete this constructor if its not used after implementing `VerticalRestClient`
    constructor(title: String, subtitle: String, iconUrl: String, segmentId: Long): this() {
        this.title = title
        this.subtitle = subtitle
        this.iconUrl = iconUrl
        this.segmentId = segmentId
    }
    @Column var title: String? = null
    @Column var subtitle: String? = null
    @Column var iconUrl: String? = null
    @Column var segmentId: Long? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
