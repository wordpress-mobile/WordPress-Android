package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
data class PostFormatModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    // Associated local site ID (could be refactored to be a FOREIGN KEY)
    @Column var siteId: Int = 0

    // Post format attributes
    @Column var slug: String? = null
    @Column var displayName: String? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
