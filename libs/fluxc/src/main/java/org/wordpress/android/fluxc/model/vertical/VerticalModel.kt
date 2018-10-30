package org.wordpress.android.fluxc.model.vertical

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
// TODO: Do we need any constraints?
class VerticalModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    // TODO: Delete this constructor if its not used after implementing `VerticalRestClient`
    constructor(name: String, verticalId: String): this() {
        this.name = name
        this.verticalId = verticalId
    }
    @Column var name: String? = null
    @Column var verticalId: String? = null

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
