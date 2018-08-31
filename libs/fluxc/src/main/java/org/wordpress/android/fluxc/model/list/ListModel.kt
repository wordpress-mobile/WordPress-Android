package org.wordpress.android.fluxc.model.list

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

@Table
@RawConstraints("FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE")
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var localSiteId: Int = 0

    // These fields shouldn't be used directly.
    @Column var typeDbValue: Int? = null
    @Column var filterDbValue: String? = null
    @Column var orderDbValue: String? = null
    @Column var stateDbValue: Int = ListState.CAN_LOAD_MORE.value

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    val listDescriptor: ListDescriptor
        get() = ListDescriptor(typeDbValue, filterDbValue, orderDbValue)

    val state: ListState
        get() = ListState.values().firstOrNull { it.value == this.stateDbValue }!!
}
