package org.wordpress.android.fluxc.model.list

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

const val LIST_STATE_TIMEOUT = 60 * 1000 // 1 minute

@Table
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    // These fields shouldn't be used directly.
    @Column var descriptorUniqueIdentifierDbValue: Int? = null
    @Column var descriptorTypeIdentifierDbValue: Int? = null
    @Column var stateDbValue: Int = ListState.defaultState.value

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
