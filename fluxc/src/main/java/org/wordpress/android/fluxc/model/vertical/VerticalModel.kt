package org.wordpress.android.fluxc.model.vertical

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
class VerticalModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var name: String? = null
    @Column var verticalId: String? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
